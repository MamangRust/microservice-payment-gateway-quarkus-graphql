package com.sanedge.merchant.repository;

import com.sanedge.merchant.entity.Merchant;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantCommandRepository implements PanacheRepository<Merchant> {

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
        return find("merchantId = ?1 AND deletedAt IS NOT NULL", merchantId).firstResult()
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
