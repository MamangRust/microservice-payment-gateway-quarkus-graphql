package com.sanedge.saldo.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.domain.requests.FindAllSaldos;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoRepository implements PanacheRepository<Saldo> {

    public Uni<PagedResult<Saldo>> findSaldos(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (?1 IS NULL OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%')))
                """;

        var panacheQuery = find(query, Sort.ascending("saldoId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Saldo> findByCardNumber(String cardNumber) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber).firstResult();
    }

    public Uni<PagedResult<Saldo>> findActiveSaldos(FindAllSaldos req) {
        return findSaldos(req);
    }

    public Uni<PagedResult<Saldo>> findTrashedSaldos(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (?1 IS NULL OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%')))
                """;

        var panacheQuery = find(query, Sort.ascending("saldoId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    @WithTransaction
    public Uni<Saldo> trashed(Long saldoId) {
        return find("saldoId = ?1 AND deletedAt IS NULL", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        saldo.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(saldo).map(v -> saldo);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Saldo> restore(Long saldoId) {
        return find("saldoId = ?1 AND deletedAt IS NOT NULL", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        saldo.setDeletedAt(null);
                        return persist(saldo).map(v -> saldo);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Integer> updateBalanceByCardNumber(String cardNumber, Long newBalance) {
        return update("totalBalance = ?1, updatedAt = ?2 WHERE cardNumber = ?3 AND deletedAt IS NULL",
                newBalance != null ? newBalance.intValue() : 0,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                cardNumber);
    }

    @WithTransaction
    public Uni<Integer> updateWithdrawByCardNumber(String cardNumber, Long withdrawAmount) {
        java.sql.Timestamp now = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
        return update("withdrawAmount = ?1, withdrawTime = ?2, updatedAt = ?3 WHERE cardNumber = ?4 AND deletedAt IS NULL",
                withdrawAmount != null ? withdrawAmount.intValue() : 0,
                now,
                now,
                cardNumber);
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long saldoId) {
        return find("saldoId = ?1", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        return delete(saldo).map(v -> true);
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
