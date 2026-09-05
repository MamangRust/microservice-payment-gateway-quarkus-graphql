package com.sanedge.topup.service;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class KafkaService {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);

    @Inject
    Vertx vertx;

    @Inject
    com.sanedge.common.chaos.ChaosManager chaosManager;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.acks", defaultValue = "1")
    String acks;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    void init() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", acks);

        producer = KafkaProducer.create(vertx, config);
        log.info("✅ KafkaProducer initialized. brokers={}", bootstrapServers);
    }

    @PreDestroy
    void destroy() {
        if (producer != null) {
            producer.close()
                    .onSuccess(v -> log.info("✅ KafkaProducer closed."))
                    .onFailure(err -> log.warn("⚠️ Error closing KafkaProducer: {}", err.getMessage()));
        }
    }

    public Uni<Void> sendMessage(String topic, String key, JsonObject payload) {
        if (payload == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Kafka payload cannot be null"));
        }
        // Phase 2 (event contract): attach the standard envelope
        // (event_id, schema_version, event_type, occurred_at) before publishing.
        JsonObject eventPayload = com.sanedge.common.event.EventEnvelope.withDefaults(payload, topic);
        return sendRaw(topic, key, eventPayload);
    }

    /**
     * Sends an already-enveloped payload verbatim (no re-wrapping). Used by the
     * outbox relay so replays keep the original event_id/occurred_at.
     */
    public Uni<Void> sendExistingEvent(String topic, String key, JsonObject eventPayload) {
        if (eventPayload == null) {
            return Uni.createFrom().failure(new IllegalArgumentException("Kafka payload cannot be null"));
        }
        return sendRaw(topic, key, eventPayload);
    }

    private Uni<Void> sendRaw(String topic, String key, JsonObject eventPayload) {
        if (producer == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("Kafka producer not initialized"));
        }

        com.sanedge.common.chaos.ChaosPolicy policy = chaosManager.evaluate("kafka", topic);
        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
            log.info("🔥 Injecting Kafka chaos [Policy: {}] for topic: {}", policy.getName(), topic);

            if (policy.getLatencyMs() > 0) {
                long delay = policy.getLatencyMs();
                return Uni.createFrom().emitter(emitter -> {
                    vertx.setTimer(delay, id -> {
                        if (policy.isDropMessage()) {
                            log.info("💧 Silent drop (latency + dropMessage) for Kafka topic: {}", topic);
                            emitter.complete(null);
                        } else if (policy.isRejectMessage()) {
                            log.warn("❌ Rejecting message (latency + rejectMessage) for Kafka topic: {}", topic);
                            emitter.fail(new RuntimeException(policy.getErrorMessage() != null ? policy.getErrorMessage() : "Simulated Kafka drop/reject error"));
                        } else {
                            KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, eventPayload.encode());
                            producer.send(record)
                                    .onSuccess(metadata -> {
                                        log.debug("Sent message to topic {}: {}", topic, eventPayload.encode());
                                        emitter.complete(null);
                                    })
                                    .onFailure(err -> {
                                        log.error("Failed to send message to topic {}", topic, err);
                                        emitter.fail(err);
                                    });
                        }
                    });
                });
            } else {
                if (policy.isDropMessage()) {
                    log.info("💧 Silent drop (dropMessage) for Kafka topic: {}", topic);
                    return Uni.createFrom().voidItem();
                } else if (policy.isRejectMessage()) {
                    log.warn("❌ Rejecting message (rejectMessage) for Kafka topic: {}", topic);
                    return Uni.createFrom().failure(new RuntimeException(policy.getErrorMessage() != null ? policy.getErrorMessage() : "Simulated Kafka drop/reject error"));
                }
            }
        }

        KafkaProducerRecord<String, String> record = KafkaProducerRecord.create(topic, key, eventPayload.encode());
        return Uni.createFrom().emitter(emitter -> {
            producer.send(record)
                    .onSuccess(metadata -> {
                        log.debug("Sent message to topic {}: {}", topic, eventPayload.encode());
                        emitter.complete(null);
                    })
                    .onFailure(err -> {
                        log.error("Failed to send message to topic {}", topic, err);
                        emitter.fail(err);
                    });
        });
    }
}
