package com.sanedge.withdraw.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.withdraw.entity.Withdraw;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WithdrawRepository implements PanacheRepository<Withdraw> {

    public Uni<PagedResult<Withdraw>> findAllWithdraws(String keyword, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR cardNumber LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Withdraw>> findActiveWithdraws(String keyword, int page, int size) {
        return findAllWithdraws(keyword, page, size);
    }

    public Uni<PagedResult<Withdraw>> findTrashedWithdraws(String keyword, int page, int size) {
        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR cardNumber LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<List<Withdraw>> findByCardNumber(String cardNumber) {
        return list("deletedAt IS NULL AND cardNumber = ?1", Sort.descending("withdrawTime"), cardNumber);
    }

    public Uni<PagedResult<Withdraw>> findAllByCardNumber(String cardNumber, String keyword, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND cardNumber = ?1
                    AND (
                        ?2 IS NULL
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?2, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?2, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), cardNumber, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    @WithTransaction
    public Uni<Withdraw> updateStatus(Long withdrawId, String status) {
        return find("withdrawId = ?1 AND deletedAt IS NULL", withdrawId).firstResult()
                .chain(withdraw -> {
                    if (withdraw != null) {
                        try {
                            withdraw.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            withdraw.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            return persist(withdraw).map(v -> withdraw);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(withdraw);
                        }
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Withdraw> trashed(Long withdrawId) {
        return find("withdrawId = ?1 AND deletedAt IS NULL", withdrawId).firstResult()
                .chain(withdraw -> {
                    if (withdraw != null) {
                        withdraw.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(withdraw).map(v -> withdraw);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Withdraw> restore(Long withdrawId) {
        return find("withdrawId = ?1 AND deletedAt IS NOT NULL", withdrawId).firstResult()
                .chain(withdraw -> {
                    if (withdraw != null) {
                        withdraw.setDeletedAt(null);
                        return persist(withdraw).map(v -> withdraw);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long withdrawId) {
        return find("withdrawId = ?1", withdrawId).firstResult()
                .chain(withdraw -> {
                    if (withdraw != null) {
                        return delete(withdraw).map(v -> true);
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
