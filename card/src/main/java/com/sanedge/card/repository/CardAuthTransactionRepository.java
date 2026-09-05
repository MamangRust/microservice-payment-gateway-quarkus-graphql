package com.sanedge.card.repository;

import java.time.Instant;
import java.util.List;

import com.sanedge.card.entity.CardAuthTransaction;
import com.sanedge.card.entity.CardAuthTransaction.AuthTxnStatus;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardAuthTransactionRepository implements PanacheRepository<CardAuthTransaction> {

    @WithTransaction
    public Uni<CardAuthTransaction> findByIdempotencyKey(String key) {
        return find("idempotencyKey = ?1", key).firstResult();
    }

    @WithTransaction
    public Uni<List<CardAuthTransaction>> findByCardNumber(String cardNumber, int page, int size) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber)
                .page(Page.of(page - 1, size))
                .list();
    }

    @WithTransaction
    public Uni<Long> countByCardNumber(String cardNumber) {
        return count("cardNumber = ?1 AND deletedAt IS NULL", cardNumber);
    }

    @WithTransaction
    public Uni<Long> countRecentByCardNumber(String cardNumber, Instant since) {
        return count("cardNumber = ?1 AND createdAt >= ?2 AND deletedAt IS NULL", cardNumber, since);
    }

    @WithTransaction
    public Uni<CardAuthTransaction> updateStatus(Long authTxnId, AuthTxnStatus status) {
        return findById(authTxnId)
                .chain(txn -> {
                    if (txn == null) {
                        return Uni.createFrom().nullItem();
                    }
                    txn.status = status;
                    if (status == AuthTxnStatus.APPROVED) {
                        txn.authorizedAt = Instant.now();
                    } else if (status == AuthTxnStatus.REVERSED) {
                        txn.reversedAt = Instant.now();
                    }
                    return persist(txn).map(v -> txn);
                });
    }

    @WithTransaction
    public Uni<CardAuthTransaction> updateRiskScore(Long authTxnId, int score) {
        return findById(authTxnId)
                .chain(txn -> {
                    if (txn == null) {
                        return Uni.createFrom().nullItem();
                    }
                    txn.riskScore = score;
                    return persist(txn).map(v -> txn);
                });
    }
}
