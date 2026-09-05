package com.sanedge.saldo.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sanedge.saldo.domain.requests.CreateSaldoRequest;
import com.sanedge.saldo.service.impl.SaldoCommandServiceImpl;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class SaldoConsumer {
    private static final Logger log = LoggerFactory.getLogger(SaldoConsumer.class);

    @Inject
    Vertx vertx;

    @Inject
    SaldoCommandServiceImpl saldoCommandService;

    private KafkaConsumer<String, JsonObject> consumer;

    void onStart(@Observes StartupEvent ev) {
        log.info("Starting Saldo Kafka Consumer...");

        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", System.getenv().getOrDefault("KAFKA_BROKERS", "localhost:9092"));
        kafkaConfig.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        kafkaConfig.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
        kafkaConfig.put("group.id", "saldo-service-group");
        kafkaConfig.put("auto.offset.reset", "earliest");

        consumer = KafkaConsumer.create(vertx, kafkaConfig);

        String topic = "saldo-service-topic-create-saldo";

        consumer.handler(record -> {
            try {
                JsonObject payload = record.value();
                log.info("Received saldo create event from topic {}: {}", record.topic(), payload.encode());

                String cardNumber = payload.getString("card_number");
                Long totalBalance = payload.getLong("total_balance", 0L);

                if (cardNumber == null) {
                    log.warn("Received incomplete saldo payload: {}", payload.encode());
                    return;
                }

                CreateSaldoRequest createRequest = new CreateSaldoRequest();
                createRequest.setCardNumber(cardNumber);
                createRequest.setTotalBalance(totalBalance);

                // Kafka handlers run on a raw Vert.x context. Mark that context as
                // safe before invoking Hibernate Reactive through @WithTransaction.
                VertxContextSafetyToggle.setCurrentContextSafe(true);
                Panache.withTransaction(() -> saldoCommandService.create(createRequest))
                        .subscribe().with(
                                res -> log.info("Saldo record successfully initialized for card {}", cardNumber),
                                err -> log.error("Failed to initialize saldo for card {}", cardNumber, err));
            } catch (Exception e) {
                log.error("Error processing saldo consumer record", e);
            }
        });

        consumer.subscribe(Collections.singleton(topic))
                .onSuccess(v -> log.info("Saldo Consumer successfully subscribed to topic: {}", topic))
                .onFailure(err -> log.error("Failed to start Saldo Consumer subscription", err));
    }

    void onStop(@Observes ShutdownEvent ev) {
        log.info("Stopping Saldo Kafka Consumer...");
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("Saldo consumer closed successfully"))
                    .onFailure(err -> log.error("Failed to close Saldo consumer", err));
        }
    }
}