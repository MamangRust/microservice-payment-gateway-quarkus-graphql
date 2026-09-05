package com.sanedge.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class EventEnvelopeTest {

    @Test
    void withDefaults_addsEnvelopeFields() {
        JsonObject event = EventEnvelope.withDefaults(
                new JsonObject().put("email", "a@b.c"), "email-service-topic-transaction-create");

        assertNotNull(event.getString("event_id"));
        assertTrue(event.getString("event_id").length() > 0);
        assertEquals(EventEnvelope.SCHEMA_VERSION, event.getInteger("schema_version"));
        assertEquals("email-service-topic-transaction-create", event.getString("event_type"));
        assertNotNull(event.getString("occurred_at"));
        assertEquals("a@b.c", event.getString("email"));
    }

    @Test
    void withDefaults_preservesExistingEventId() {
        JsonObject event = EventEnvelope.withDefaults(
                new JsonObject().put("event_id", "stable-id"), "topic");

        assertEquals("stable-id", event.getString("event_id"));
    }

    @Test
    void withDefaults_doesNotMutateInput() {
        JsonObject input = new JsonObject().put("email", "a@b.c");
        EventEnvelope.withDefaults(input, "topic");

        assertFalse(input.containsKey("event_id"));
    }

    @Test
    void withDefaults_nullReturnsNull() {
        assertNull(EventEnvelope.withDefaults(null, "topic"));
    }
}
