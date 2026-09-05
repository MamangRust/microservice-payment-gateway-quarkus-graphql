package com.sanedge.transaction.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionRepository implements PanacheRepository<Transaction> {

    public Uni<PagedResult<Transaction>> findTransactions(String search, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transactionTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transaction>> findActiveTransactions(String search, int page, int size) {
        return findTransactions(search, page, size);
    }

    public Uni<PagedResult<Transaction>> findTrashedTransactions(String search, int page, int size) {
        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transactionTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Transaction> findTransactionById(Long id) {
        return find("transactionId = ?1 AND deletedAt IS NULL", id).firstResult();
    }

    public Uni<PagedResult<Transaction>> findTransactionsByCardNumber(String cardNumber, String filter, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND cardNumber = ?1
                    AND (
                        ?2 IS NULL
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?2, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transactionTime"), cardNumber, filter)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<List<Transaction>> findTransactionsByMerchantId(Long merchantId) {
        return list("merchantId = ?1 AND deletedAt IS NULL", Sort.descending("transactionTime"), merchantId != null ? merchantId.intValue() : 0);
    }

    @WithTransaction
    public Uni<Transaction> updateTransactionStatus(Long transactionId, String status) {
        return find("transactionId = ?1 AND deletedAt IS NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        try {
                            transaction.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            transaction.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            return persist(transaction).map(v -> transaction);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(transaction);
                        }
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transaction> trashed(Long transactionId) {
        return find("transactionId = ?1 AND deletedAt IS NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        transaction.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transaction> restore(Long transactionId) {
        return find("transactionId = ?1 AND deletedAt IS NOT NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        transaction.setDeletedAt(null);
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long transactionId) {
        return find("transactionId = ?1", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        return delete(transaction).map(v -> true);
                    }
                    return Uni.createFrom().item(false);
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }
}
