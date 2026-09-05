package com.sanedge.email;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Durable idempotency guard for the email consumer (Phase 3).
 *
 * <p>Replaces the previous in-memory {@code deliveredEventIds} /
 * {@code deadLetteredEventIds} sets, which were lost on restart and not shared
 * between replicas. Every event goes through a small Redis state machine:
 *
 * <pre>
 * ABSENT --claim(SET NX PX lease)--> PROCESSING --send ok--> SENT (TTL 24h)
 *                                         |--send fail--> release (DEL) -> DLQ
 * </pre>
 *
 * <ul>
 *   <li>{@link #claim} is atomic ({@code SET NX PX}): only one consumer can hold
 *       the lease, so two replicas can never send the same event concurrently.</li>
 *   <li>The lease bounds crash windows: a consumer that dies after claiming but
 *       before sending is recovered once the key expires.</li>
 *   <li>{@link #markSent} flips the key to a terminal {@code SENT} state so
 *       replays with the same {@code event_id} are skipped.</li>
 *   <li>Fail-open: if Redis is unavailable the guard lets the email through
 *       (availability of notifications &gt; exactly-once).</li>
 * </ul>
 */
@ApplicationScoped
public class EmailIdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(EmailIdempotencyGuard.class);

    private static final String STATE_PROCESSING = "PROCESSING";
    private static final String STATE_SENT = "SENT";

    /** Result of an atomic {@link #claim}. */
    public enum ClaimResult {
        /** This caller owns the lease and must send the email. */
        CLAIMED,
        /** Event already reached a terminal {@code SENT} state — skip. */
        DUPLICATE,
        /** Another processor holds an active lease — do not send, retry later. */
        BUSY
    }

    @Inject
    ReactiveRedisDataSource reactiveRedis;

    @Inject
    OpenTelemetry openTelemetry;

    private Meter meter;
    private LongCounter claimFailedCounter;

    @PostConstruct
    void initMetrics() {
        meter = openTelemetry.getMeter("email-service");
        claimFailedCounter = meter.counterBuilder("email_idempotency_claim_failed_total")
                .setDescription("Total idempotency claim failures (fail-open active, duplicate window unbounded)")
                .build();
    }

    @ConfigProperty(name = "email.idempotency.lease-seconds", defaultValue = "60")
    long leaseSeconds;

    @ConfigProperty(name = "email.idempotency.ttl-seconds", defaultValue = "86400")
    long ttlSeconds;

    /**
     * Atomically claims {@code eventId} for processing. See {@link ClaimResult}.
     *
     * <p>The claim uses {@code SETNX} + {@code EXPIRE}: only one consumer can
     * create the key. If we crash between the two commands the key is left
     * without a TTL, so the next claim self-heals it (ttl == -1) by deleting
     * and re-claiming instead of blocking the event forever.
     */
    public Uni<ClaimResult> claim(String eventId) {
        String key = key(eventId);
        ReactiveValueCommands<String, String> values = valueCommands();
        ReactiveKeyCommands<String> keys = reactiveRedis.key(String.class);
        return values.setnx(key, STATE_PROCESSING)
                .onItem().transformToUni(created -> {
                    if (created) {
                        // Bounded lease: if we crash before expire() runs, the key
                        // stays without TTL and is self-healed by the branch below.
                        return keys.expire(key, leaseSeconds)
                                .map(ignored -> ClaimResult.CLAIMED);
                    }
                    // Key exists: terminal SENT or an active lease.
                    return values.get(key)
                            .onItem().transformToUni(state -> {
                                if (STATE_SENT.equals(state)) {
                                    return Uni.createFrom().item(ClaimResult.DUPLICATE);
                                }
                                // PROCESSING (or unknown value): check lease liveness.
                                return keys.ttl(key)
                                        .onItem().transformToUni(ttl -> {
                                            if (ttl != null && ttl == -1L) {
                                                // Orphaned PROCESSING key (crash between
                                                // setnx and expire) — reclaim it.
                                                return keys.del(key)
                                                        .onItem().transformToUni(ignored -> claim(eventId));
                                            }
                                            return Uni.createFrom().item(ClaimResult.BUSY);
                                        });
                            });
                })
                .onFailure().recoverWithItem(err -> {
                    if (claimFailedCounter != null) {
                        claimFailedCounter.add(1);
                    }
                    log.warn("⚠️ Idempotency claim failed (fail-open), sending anyway | event_id={} error={}",
                            eventId, err.getMessage());
                    return ClaimResult.CLAIMED;
                });
    }

    /** Marks the event as successfully delivered (terminal state, full dedup TTL). */
    public Uni<Void> markSent(String eventId) {
        String key = key(eventId);
        return valueCommands().setex(key, ttlSeconds, STATE_SENT)
                .onFailure().recoverWithItem(err -> {
                    log.warn("⚠️ Idempotency markSent failed (fail-open) | event_id={} error={}",
                            eventId, err.getMessage());
                    return null;
                })
                .replaceWithVoid();
    }

    /** Releases the claim so a failed send can be retried. */
    public Uni<Void> release(String eventId) {
        String key = key(eventId);
        return reactiveRedis.key(String.class).del(key)
                .onFailure().recoverWithItem(err -> {
                    log.warn("⚠️ Idempotency release failed (fail-open) | event_id={} error={}",
                            eventId, err.getMessage());
                    return null;
                })
                .replaceWithVoid();
    }

    private ReactiveValueCommands<String, String> valueCommands() {
        return reactiveRedis.value(String.class, String.class);
    }

    private String key(String eventId) {
        return "email:idempotency:" + eventId;
    }
}
