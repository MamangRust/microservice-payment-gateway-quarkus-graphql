package com.sanedge.card.kafka;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.repository.CardEventLogRepository;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Phase 4 event audit sink: consumes the card domain events that were
 * previously publish-only ({@code card.payment.posted},
 * {@code card.statement.generated}, {@code card.fraud.alert}) and persists them
 * into {@code card_event_logs} (PostgreSQL) via {@link CardEventLogRepository}.
 *
 * <p>Consumer group {@code card-event-log-group}, {@code auto.offset.reset=earliest}.
 * Redelivery is idempotent at the DB level (find-before-insert + partial unique
 * index from migration V22), so {@code enable.auto.commit=true} is acceptable
 * for this audit sink.</p>
 */
@ApplicationScoped
public class CardEventLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(CardEventLogConsumer.class);
    private static final String GROUP_ID = "card-event-log-group";

    private static final Set<String> TOPICS = Set.of(
            "card.payment.posted",
            "card.statement.generated",
            "card.fraud.alert");

    @Inject
    Vertx vertx;

    @Inject
    CardEventLogRepository eventLogRepo;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    private KafkaConsumer<String, String> consumer;

    void onStart(@Observes StartupEvent ev) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", GROUP_ID);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "true");

        consumer = KafkaConsumer.create(vertx, config);

        consumer.handler(this::handleRecord);
        consumer.subscribe(TOPICS)
                .onSuccess(v -> log.info("✅ CardEventLogConsumer subscribed to {} topics", TOPICS.size()))
                .onFailure(err -> log.error("❌ CardEventLogConsumer subscription failed", err));
    }

    void onStop(@Observes ShutdownEvent ev) {
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("✅ CardEventLogConsumer closed."))
                    .onFailure(err -> log.warn("⚠️ Error closing CardEventLogConsumer: {}", err.getMessage()));
        }
    }

    void handleRecord(KafkaConsumerRecord<String, String> record) {
        try {
            String topic = record.topic();
            JsonObject payload = new JsonObject(record.value());
            String referenceId = resolveReferenceId(topic, payload);

            eventLogRepo.appendIfAbsent(topic, resolveEventType(topic), resolveCardNumber(payload), referenceId,
                    record.value())
                    .subscribe().with(
                            saved -> log.debug("Event logged topic={} referenceId={}", topic, referenceId),
                            err -> log.error("Failed to log event topic={} referenceId={}", topic, referenceId, err));
        } catch (Exception e) {
            log.error("Error processing event-log record from topic {}", record.topic(), e);
        }
    }

    String resolveEventType(String topic) {
        // Event type mirrors the domain topic until an explicit envelope is adopted.
        return topic;
    }

    String resolveCardNumber(JsonObject payload) {
        return payload.getString("cardNumber");
    }

    /**
     * Stable per-entity reference used for at-least-once dedup.
     * {@code card.statement.generated} deliberately returns {@code null} (see
     * {@link CardEventLogRepository#appendIfAbsent}).
     */
    String resolveReferenceId(String topic, JsonObject payload) {
        switch (topic) {
            case "card.payment.posted":
                return stringValue(payload.getValue("paymentId"));
            case "card.fraud.alert":
                return stringValue(payload.getValue("authTxnId"));
            default:
                return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
