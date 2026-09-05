package com.sanedge.card.kafka;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.repository.CardAuthTransactionRepository;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.KafkaService;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class FraudScoringConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringConsumer.class);

    @Inject
    Vertx vertx;

    @Inject
    CardAuthTransactionRepository authTxnRepo;

    @Inject
    CardCommandRepository cardCommandRepo;

    @Inject
    CardQueryRepository cardQueryRepository;

    @Inject
    KafkaService kafkaService;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.group.id", defaultValue = "card-fraud-scoring-group")
    String groupId;

    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    void init() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "true");

        consumer = KafkaConsumer.create(vertx, config);

        consumer.subscribe("card.txn.created")
                .onSuccess(v -> log.info("✅ FraudScoringConsumer subscribed to card.txn.created"))
                .onFailure(err -> log.error("❌ FraudScoringConsumer subscription failed", err));

        consumer.handler(this::handleMessage);
    }

    @PreDestroy
    void destroy() {
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("✅ FraudScoringConsumer closed."))
                    .onFailure(err -> log.warn("⚠️ Error closing FraudScoringConsumer: {}", err.getMessage()));
        }
    }

    private void handleMessage(KafkaConsumerRecord<String, String> record) {
        try {
            JsonObject json = new JsonObject(record.value());
            Long authTxnId = json.getLong("authTxnId");
            String cardNumber = json.getString("cardNumber");
            BigDecimal amount = new BigDecimal(json.getString("amount", "0"));
            String mcc = json.getString("mcc");

            log.debug("FraudScoring processing txn={}, card={}, amount={}", authTxnId, cardNumber, amount);

            int score = computeRiskScore(amount, mcc);

            authTxnRepo.updateRiskScore(authTxnId, score)
                    .chain(txn -> {
                        if (score > 70 && txn != null) {
                            return cardQueryRepository.findByCardNumber(cardNumber)
                                    .chain(card -> {
                                        if (card != null) {
                                            card.status = CardStatus.BLOCKED;
                                            return cardCommandRepo.persist(card).map(v -> card);
                                        }
                                        return UniCreateVoid();
                                    })
                                    .chain(card -> {
                                        JsonObject alert = new JsonObject()
                                                .put("authTxnId", authTxnId)
                                                .put("cardNumber", cardNumber)
                                                .put("riskScore", score)
                                                .put("reason", "High risk score > 70");
                                        return kafkaService.sendMessage("card.fraud.alert", cardNumber, alert);
                                    });
                        } else if (score >= 30) {
                            JsonObject alert = new JsonObject()
                                    .put("authTxnId", authTxnId)
                                    .put("cardNumber", cardNumber)
                                    .put("riskScore", score)
                                    .put("reason", "Medium risk - flag for review");
                            return kafkaService.sendMessage("card.fraud.alert", cardNumber, alert);
                        }
                        return UniCreateVoid();
                    })
                    .subscribe().with(
                            v -> log.debug("Fraud scoring completed for txn={}", authTxnId),
                            err -> log.error("Fraud scoring failed for txn={}", authTxnId, err));
        } catch (Exception e) {
            log.error("Error processing fraud scoring message", e);
        }
    }

    private io.smallrye.mutiny.Uni<Void> UniCreateVoid() {
        return io.smallrye.mutiny.Uni.createFrom().voidItem();
    }

    private int computeRiskScore(BigDecimal amount, String mcc) {
        int score = 0;
        BigDecimal tenMillion = new BigDecimal("10000000");
        BigDecimal fiveMillion = new BigDecimal("5000000");
        BigDecimal oneMillion = new BigDecimal("1000000");

        if (amount.compareTo(tenMillion) > 0) {
            score += 50;
        } else if (amount.compareTo(fiveMillion) > 0) {
            score += 30;
        } else if (amount.compareTo(oneMillion) > 0) {
            score += 15;
        }

        Set<String> blacklistedMcc = Set.of("7995", "5967", "7273", "4829");
        if (mcc != null && blacklistedMcc.contains(mcc)) {
            score += 40;
        }

        return Math.min(score, 100);
    }
}
