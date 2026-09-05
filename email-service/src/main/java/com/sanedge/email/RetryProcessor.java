package com.sanedge.email;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Drains {@code email-service-topic-email-retry} (Phase 4). Records carry the
 * original source coordinates ({@code _srcTopic/_srcPartition/_srcOffset}), the
 * attempt counter ({@code _attempt}) and a {@code _retryAt} timestamp so backoff
 * delays are honoured. Records past {@code kafka.email.max-retries} are routed
 * to the shared DLQ by {@link EmailService#processRecord}.
 */
@ApplicationScoped
public class RetryProcessor {

    private static final Logger log = LoggerFactory.getLogger(RetryProcessor.class);

    private static final List<String> INTERNAL_FIELDS = Arrays.asList(
            "_srcTopic", "_srcPartition", "_srcOffset", "_attempt", "_retryAt", "_reason");

    @Inject
    Vertx vertx;

    @Inject
    EmailService emailService;

    private KafkaConsumer<String, JsonObject> consumer;
    private final Map<TopicPartition, CompletableFuture<Void>> partitionTails = new ConcurrentHashMap<>();

    void onStart(@Observes StartupEvent ev) {
        Map<String, String> config = emailService.consumerConfig();
        config.put("group.id", "email-service-retry-group");

        consumer = KafkaConsumer.create(vertx, config);
        consumer.handler(this::handleRecord);
        consumer.subscribe(Collections.singleton(EmailService.RETRY_TOPIC))
                .onSuccess(v -> log.info("🔁 RetryProcessor subscribed to {}", EmailService.RETRY_TOPIC))
                .onFailure(err -> log.error("❌ Failed to subscribe RetryProcessor", err));
    }

    private void handleRecord(KafkaConsumerRecord<String, JsonObject> record) {
        TopicPartition partition = new TopicPartition(record.topic(), record.partition());
        CompletableFuture<Void> previous = partitionTails.getOrDefault(partition,
                CompletableFuture.completedFuture(null));
        Span span = emailService.startConsumeSpan(record.headers(), record.topic(),
                record.partition(), record.offset());
        CompletableFuture<Void> current;
        try (Scope scope = span.makeCurrent()) {
            current = previous
                    .thenCompose(ignored -> processAndCommit(record)
                            // Retry records stay ordered per partition. A failed retry/DLQ
                            // publish must leave the offset uncommitted.
                            .onFailure().retry()
                            .withBackOff(Duration.ofMillis(250), Duration.ofSeconds(5))
                            .indefinitely()
                            .subscribeAsCompletionStage())
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            span.setStatus(StatusCode.ERROR, error.getMessage());
                            log.error("❌ Retry processing failed | topic={} partition={} offset={} error={}",
                                    record.topic(), record.partition(), record.offset(), error.getMessage());
                        }
                        span.end();
                    });
        } catch (Exception error) {
            span.recordException(error);
            span.end();
            log.error("❌ Unhandled error in retry handler", error);
            return;
        }
        partitionTails.put(partition, current);
    }

    private Uni<Void> processAndCommit(KafkaConsumerRecord<String, JsonObject> record) {
        return waitUntilRetryAt(record.value())
                .chain(ignored -> process(record))
                .chain(ignored -> commit(record));
    }

    private Uni<Void> waitUntilRetryAt(JsonObject payload) {
        if (payload == null) {
            return Uni.createFrom().voidItem();
        }
        long delay = payload.getLong("_retryAt", 0L) - System.currentTimeMillis();
        if (delay <= 0) {
            return Uni.createFrom().voidItem();
        }
        return Uni.createFrom().emitter(emitter -> vertx.setTimer(delay, ignored -> emitter.complete(null)));
    }

    private Uni<Void> process(KafkaConsumerRecord<String, JsonObject> record) {
        JsonObject payload = record.value();
        if (payload == null) {
            return Uni.createFrom().voidItem();
        }

        String srcTopic = payload.getString("_srcTopic");
        int srcPartition = payload.getInteger("_srcPartition", 0);
        long srcOffset = payload.getLong("_srcOffset", 0L);
        int attempt = payload.getInteger("_attempt", 1);

        JsonObject clean = payload.copy();
        for (String field : INTERNAL_FIELDS) {
            clean.remove(field);
        }

        return emailService.processRecord(srcTopic, srcPartition, srcOffset,
                record.key(), clean, attempt);
    }

    private Uni<Void> commit(KafkaConsumerRecord<String, JsonObject> record) {
        Map<TopicPartition, OffsetAndMetadata> offsets = Map.of(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1, null));
        return Uni.createFrom().completionStage(consumer.commit(offsets).toCompletionStage())
                .replaceWithVoid()
                .invoke(() -> log.debug("Committed retry offset topic={} partition={} offset={}",
                        record.topic(), record.partition(), record.offset()));
    }

    void onStop(@Observes ShutdownEvent ev) {
        partitionTails.clear();
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("🔁 RetryProcessor consumer closed"))
                    .onFailure(err -> log.error("❌ Failed to close RetryProcessor consumer", err));
        }
    }
}
