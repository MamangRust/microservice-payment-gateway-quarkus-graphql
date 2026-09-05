package com.sanedge.statswriter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

/**
 * In-memory at-least-once dedup keyed by the outbox {@code event_id}. Mirrors
 * the Go reference ({@code idempotent_consumer.Dedup}, 48h window) so Kafka
 * redeliveries/outbox retries never double-insert into ClickHouse.
 */
@Singleton
public class EventDedup {

    private final long windowMillis;

    private final Map<String, Long> seen = new ConcurrentHashMap<>();

    public EventDedup() {
        this(48L * 60 * 60 * 1000);
    }

    public EventDedup(long windowMillis) {
        this.windowMillis = windowMillis;
    }

    /**
     * Returns {@code true} if this event id has already been processed within
     * the dedup window.
     */
    public boolean isDuplicate(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long first = seen.putIfAbsent(eventId, now);
        return first != null;
    }

    /** Evicts entries older than the window (called periodically). */
    public void purgeExpired() {
        long cutoff = System.currentTimeMillis() - windowMillis;
        seen.entrySet().removeIf(e -> e.getValue() < cutoff);
    }

    public int size() {
        return seen.size();
    }
}
