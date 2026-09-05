package com.sanedge.common.event;

import java.time.Instant;
import java.util.UUID;

import io.vertx.core.json.JsonObject;

/**
 * Standard event envelope shared by all producers that publish to Kafka.
 *
 * <p>Every message carries a stable {@code event_id} (so consumers can
 * deduplicate replays and retries), a {@code schema_version}, the logical
 * {@code event_type} and an {@code occurred_at} timestamp. An existing
 * {@code event_id} is always preserved — outbox replay and retry must keep
 * the same id for idempotency.
 */
public final class EventEnvelope {

    public static final int SCHEMA_VERSION = 1;

    private EventEnvelope() {
    }

    /**
     * Returns a copy of {@code payload} with the standard envelope fields
     * added. A caller-supplied {@code event_id} is never overwritten.
     */
    public static JsonObject withDefaults(JsonObject payload, String eventType) {
        if (payload == null) {
            return null;
        }
        JsonObject event = payload.copy();
        if (event.getString("event_id") == null || event.getString("event_id").isBlank()) {
            event.put("event_id", UUID.randomUUID().toString());
        }
        event.put("schema_version", SCHEMA_VERSION);
        event.put("event_type", eventType);
        event.put("occurred_at", Instant.now().toString());
        return event;
    }
}
