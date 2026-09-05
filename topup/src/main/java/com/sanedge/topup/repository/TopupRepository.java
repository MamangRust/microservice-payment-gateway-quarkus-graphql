package com.sanedge.topup.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.domain.requests.FindAllTopups;
import com.sanedge.topup.domain.requests.FindAllTopupsByCardNumber;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopupRepository implements PanacheRepository<Topup> {

    public Uni<PagedResult<Topup>> findTopups(FindAllTopups req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(topupNo AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(topupMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("topupTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Topup>> findTopupByCard(FindAllTopupsByCardNumber req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        String cardNumber = req.getCardNumber();

        var query = """
                    deletedAt IS NULL
                    AND cardNumber = ?1
                    AND (
                        ?2 IS NULL
                        OR LOWER(CAST(topupNo AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                        OR LOWER(topupMethod) LIKE LOWER(CONCAT('%', ?2, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("topupTime"), cardNumber, search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Topup>> findActiveTopups(FindAllTopups req) {
        return findTopups(req);
    }

    public Uni<PagedResult<Topup>> findTrashedTopups(FindAllTopups req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(topupNo AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(topupMethod) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("topupTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<List<Topup>> findByCardNumber(String cardNumber) {
        return list("cardNumber = ?1 AND deletedAt IS NULL", Sort.descending("topupTime"), cardNumber);
    }

    public Uni<Topup> findTopupById(Long id) {
        return find("topupId = ?1 AND deletedAt IS NULL", id).firstResult();
    }

    @WithTransaction
    public Uni<Topup> updateTopupAmount(Long topupId, Long amount) {
        return find("topupId = ?1 AND deletedAt IS NULL", topupId).firstResult()
                .chain(topup -> {
                    if (topup != null) {
                        topup.topupAmount = amount != null ? amount.intValue() : 0;
                        topup.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(topup).map(v -> topup);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Topup> updateTopupStatus(Long topupId, String status) {
        return find("topupId = ?1 AND deletedAt IS NULL", topupId).firstResult()
                .chain(topup -> {
                    if (topup != null) {
                        try {
                            topup.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            topup.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            return persist(topup).map(v -> topup);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(topup);
                        }
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Topup> trashed(Long topupId) {
        return find("topupId = ?1 AND deletedAt IS NULL", topupId).firstResult()
                .chain(topup -> {
                    if (topup != null) {
                        topup.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(topup).map(v -> topup);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Topup> restore(Long topupId) {
        return find("topupId = ?1 AND deletedAt IS NOT NULL", topupId).firstResult()
                .chain(topup -> {
                    if (topup != null) {
                        topup.setDeletedAt(null);
                        return persist(topup).map(v -> topup);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long topupId) {
        return find("topupId = ?1", topupId).firstResult()
                .chain(topup -> {
                    if (topup != null) {
                        return delete(topup).map(v -> true);
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
