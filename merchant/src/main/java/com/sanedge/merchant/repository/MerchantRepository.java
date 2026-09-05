package com.sanedge.merchant.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant.entity.Merchant;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantRepository implements PanacheRepository<Merchant> {

    public Uni<Boolean> existsByName(String name) {
        return count("LOWER(name) = LOWER(?1) AND deletedAt IS NULL", name).map(c -> c > 0);
    }

    public Uni<PagedResult<Merchant>> findMerchants(String keyword, int page, int size) {
        var query = """
                    ?1 IS NULL
                    OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                """;

        var panacheQuery = find(query, Sort.ascending("merchantId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Merchant>> findActiveMerchants(String keyword, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.ascending("merchantId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Merchant>> findTrashedMerchants(String keyword, int page, int size) {
        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(name) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(apiKey) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("merchantId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Merchant> findMerchantById(Long merchantId) {
        return find("merchantId = ?1 AND deletedAt IS NULL", merchantId).firstResult();
    }

    public Uni<Merchant> findByApiKey(String apiKey) {
        return find("apiKey = ?1 AND deletedAt IS NULL", apiKey).firstResult();
    }

    public Uni<Merchant> findByName(String name) {
        return find("name = ?1 AND deletedAt IS NULL", name).firstResult();
    }

    public Uni<List<Merchant>> findByUserId(Long userId) {
        if (userId == null) {
            return Uni.createFrom().item(List.of());
        }
        return list("userId = ?1 AND deletedAt IS NULL", userId.intValue());
    }

    @WithTransaction
    public Uni<Merchant> trashed(Long merchantId) {
        return find("merchantId = ?1 AND deletedAt IS NULL", merchantId).firstResult()
                .chain(merchant -> {
                    if (merchant != null) {
                        merchant.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(merchant).map(v -> merchant);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Merchant> restore(Long merchantId) {
        return find("merchantId = ?1 AND deletedAt IS NOT NULL", merchantId).firstResult()
                .chain(merchant -> {
                    if (merchant != null) {
                        merchant.setDeletedAt(null);
                        return persist(merchant).map(v -> merchant);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long merchantId) {
        return find("merchantId = ?1", merchantId).firstResult()
                .chain(merchant -> {
                    if (merchant != null) {
                        return delete(merchant).map(v -> true);
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

    @WithTransaction
    public Uni<Boolean> updateStatus(Long merchantId, String status) {
        return find("merchantId = ?1 AND deletedAt IS NULL", merchantId).firstResult()
                .chain(merchant -> {
                    if (merchant != null) {
                        try {
                            merchant.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            return persist(merchant).map(v -> true);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(false);
                        }
                    }
                    return Uni.createFrom().item(false);
                });
    }
}
