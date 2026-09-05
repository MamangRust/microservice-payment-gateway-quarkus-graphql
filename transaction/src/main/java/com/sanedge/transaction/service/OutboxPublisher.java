package com.sanedge.transaction.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.transaction.entity.Outbox;
import com.sanedge.transaction.repository.OutboxRepository;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Transactional outbox relay (transaction module). Polls PENDING rows owned by
 * the {@code transaction} domain, claims them with a lease (so concurrent
 * replicas and sibling finance modules sharing the same table never publish
 * the same row twice) and pushes them to Kafka. Failures are retried with
 * exponential backoff; the row stays PENDING until the lease expires.
 */
@ApplicationScoped
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private static final String DOMAIN = "transaction";

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    KafkaService kafkaService;

    @ConfigProperty(name = "outbox.publisher.batch-size", defaultValue = "50")
    int batchSize;

    private final AtomicBoolean polling = new AtomicBoolean();
    private final AtomicLong backlog = new AtomicLong(0);

    /**
     * Scheduled methods run on a duplicated Vert.x context, so the reactive
     * Panache calls below are safe (a raw Vert.x timer from the main thread
     * would not have a context).
     */
    @Scheduled(every = "1s", delay = 5, delayUnit = TimeUnit.SECONDS)
    Uni<Void> poll() {
        if (!polling.compareAndSet(false, true)) {
            return Uni.createFrom().voidItem();
        }
        return publishDue()
                .onItem().transformToUni(v -> outboxRepository.countPending(DOMAIN))
                .invoke(count -> {
                    backlog.set(count);
                    polling.set(false);
                })
                .replaceWithVoid()
                .onFailure().invoke(err -> {
                    log.error("Outbox poll failed", err);
                    polling.set(false);
                });
    }

    private Uni<Void> publishDue() {
        return outboxRepository.findDue(DOMAIN, Math.max(1, batchSize))
                .chain(this::publishSequentially);
    }

    private Uni<Void> publishSequentially(List<Outbox> events) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (Outbox event : events) {
            chain = chain.chain(() -> outboxRepository.claim(DOMAIN, event)
                    .chain(claimed -> {
                        if (claimed == null) {
                            return Uni.createFrom().voidItem();
                        }
                        return kafkaService.sendExistingEvent(claimed.getTopic(), claimed.getEventKey(),
                                new JsonObject(claimed.getPayload()))
                                .chain(() -> outboxRepository.markSent(claimed))
                                .invoke(() -> log.debug("Outbox published | id={} topic={} key={}",
                                        claimed.getId(), claimed.getTopic(), claimed.getEventKey()))
                                .onFailure().recoverWithUni(error -> outboxRepository.markRetry(claimed, error,
                                        retryDelaySeconds(claimed.getAttempts())));
                    }));
        }
        return chain;
    }

    private long retryDelaySeconds(int attempts) {
        return Math.min(300, 1L << Math.min(8, Math.max(0, attempts - 1)));
    }
}
