package com.sanedge.card.repository;

import java.math.BigDecimal;
import java.time.Instant;

import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Card.CardStatus;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardCommandRepository implements PanacheRepository<Card> {

    @WithTransaction
    public Uni<Card> trashed(Long cardId) {
        return find("cardId = ?1 AND deletedAt IS NULL", cardId).firstResult()
                .chain(card -> {
                    if (card != null) {
                        card.setDeletedAt(Instant.now());
                        return persist(card).map(v -> card);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Card> restore(Long cardId) {
        return find("cardId = ?1 AND deletedAt IS NOT NULL", cardId).firstResult()
                .chain(card -> {
                    if (card != null) {
                        card.setDeletedAt(null);
                        return persist(card).map(v -> card);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Card> deletePermanent(Long cardId) {
        return find("cardId = ?1 AND deletedAt IS NOT NULL", cardId).firstResult()
                .chain(card -> {
                    if (card != null) {
                        return delete(card).map(v -> card);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(updatedCount -> updatedCount > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(deletedCount -> deletedCount > 0);
    }

    @WithTransaction
    public Uni<Card> toggleStatus(Long cardId) {
        return find("cardId = ?1 AND deletedAt IS NULL", cardId).firstResult()
                .chain(card -> {
                    if (card == null) {
                        return Uni.createFrom().nullItem();
                    }
                    if (card.status == CardStatus.ACTIVE) {
                        card.status = CardStatus.SUSPENDED;
                    } else if (card.status == CardStatus.SUSPENDED) {
                        card.status = CardStatus.ACTIVE;
                    } else {
                        return Uni.createFrom().nullItem();
                    }
                    return persist(card).map(v -> card);
                });
    }

    @WithTransaction
    public Uni<Card> updateCreditLimit(Long cardId, BigDecimal limit) {
        return find("cardId = ?1 AND deletedAt IS NULL", cardId).firstResult()
                .chain(card -> {
                    if (card == null) {
                        return Uni.createFrom().nullItem();
                    }
                    card.creditLimit = limit;
                    return persist(card).map(v -> card);
                });
    }

    @WithTransaction
    public Uni<Card> redeemPoints(Long cardId, BigDecimal pointsToRedeem) {
        return find("cardId = ?1 AND deletedAt IS NULL", cardId).firstResult()
                .chain(card -> {
                    if (card == null) {
                        return Uni.createFrom().nullItem();
                    }
                    if (card.points.compareTo(pointsToRedeem) < 0) {
                        return Uni.createFrom().nullItem();
                    }
                    card.points = card.points.subtract(pointsToRedeem);
                    return persist(card).map(v -> card);
                });
    }
}