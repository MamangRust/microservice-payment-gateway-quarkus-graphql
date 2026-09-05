package com.sanedge.transaction.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionQueryRepository implements PanacheRepository<Transaction> {

    public Uni<PagedResult<Transaction>> findTransactions(FindAllTransactions req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

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

    public Uni<PagedResult<Transaction>> findActiveTransactions(FindAllTransactions req) {
        return findTransactions(req);
    }

    public Uni<PagedResult<Transaction>> findTrashedTransactions(FindAllTransactions req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

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

    public Uni<PagedResult<Transaction>> findTransactionsByCardNumber(FindAllTransactionCardNumber req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        String cardNumber = req.getCardNumber();

        var query = """
                    deletedAt IS NULL
                    AND cardNumber = ?1
                    AND (
                        ?2 IS NULL
                        OR LOWER(paymentMethod) LIKE LOWER(CONCAT('%', ?2, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transactionTime"), cardNumber, search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<List<Transaction>> findTransactionsByMerchantId(Long merchantId) {
        return list("merchantId = ?1 AND deletedAt IS NULL", Sort.descending("transactionTime"), merchantId != null ? merchantId.intValue() : 0);
    }

    public Uni<Void> lockIdempotencyKey(String key) {
        return getSession()
                .chain(session -> session.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))", Object.class)
                        .setParameter("key", key)
                        .getSingleResult())
                .replaceWithVoid();
    }

    public Uni<Transaction> findByIdempotencyKey(String key) {
        return find("idempotencyKey = ?1", key).firstResult();
    }

    public Uni<java.util.List<Transaction>> findPendingCompensation(int maxAttempts) {
        return list("status = ?1 AND deletedAt IS NULL AND compensationAttempts < ?2",
                com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts);
    }
}
