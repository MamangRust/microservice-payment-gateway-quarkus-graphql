package com.sanedge.card.repository;

import com.sanedge.card.entity.CardEventLog;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardEventLogRepository implements PanacheRepository<CardEventLog> {

    @WithTransaction
    public Uni<CardEventLog> findByTopicReference(String topic, String referenceId) {
        return find("topic = ?1 AND referenceId = ?2", topic, referenceId).firstResult();
    }

    /**
     * Idempotent append for at-least-once Kafka delivery.
     *
     * <p>Topics with a stable per-entity reference ({@code card.payment.posted}
     * → paymentId, {@code card.fraud.alert} → authTxnId) are deduplicated by
     * {@code (topic, reference_id)}: a redelivery returns the existing row
     * without inserting a duplicate. Topics without a stable reference
     * ({@code card.statement.generated}) always insert — its cycle-day key
     * repeats every month, so full dedup would wrongly drop valid events.</p>
     *
     * <p>The DB partial unique index from migration V22 is the second line of
     * defense for concurrent consumers racing on the same reference.</p>
     */
    @WithTransaction
    public Uni<CardEventLog> appendIfAbsent(String topic, String eventType, String cardNumber,
            String referenceId, String payload) {
        if (referenceId == null || referenceId.isBlank()) {
            return persist(newLog(topic, eventType, cardNumber, referenceId, payload));
        }
        return findByTopicReference(topic, referenceId)
                .chain(existing -> {
                    if (existing != null) {
                        return Uni.createFrom().item(existing);
                    }
                    // A concurrent replica may have inserted the same reference
                    // between our find and insert: resolve the constraint
                    // violation to the existing row instead of logging an error.
                    return persist(newLog(topic, eventType, cardNumber, referenceId, payload))
                            .onFailure().recoverWithUni(err -> findByTopicReference(topic, referenceId)
                                    .chain(existingRow -> existingRow != null
                                            ? Uni.createFrom().item(existingRow)
                                            : Uni.createFrom().failure(err)));
                });
    }

    private CardEventLog newLog(String topic, String eventType, String cardNumber,
            String referenceId, String payload) {
        CardEventLog log = new CardEventLog();
        log.topic = topic;
        log.eventType = eventType;
        log.cardNumber = cardNumber;
        log.referenceId = referenceId;
        log.payload = payload;
        return log;
    }
}
