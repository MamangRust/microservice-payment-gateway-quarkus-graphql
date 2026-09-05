package com.sanedge.merchant.repository;

import com.sanedge.merchant.entity.MerchantDocument;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantDocumentCommandRepository implements PanacheRepository<MerchantDocument> {

    @WithTransaction
    public Uni<MerchantDocument> trashed(Long documentId) {
        return find("documentId = ?1 AND deletedAt IS NULL", documentId).firstResult()
                .chain(doc -> {
                    if (doc != null) {
                        doc.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(doc).map(v -> doc);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<MerchantDocument> restore(Long documentId) {
        return find("documentId = ?1 AND deletedAt IS NOT NULL", documentId).firstResult()
                .chain(doc -> {
                    if (doc != null) {
                        doc.setDeletedAt(null);
                        return persist(doc).map(v -> doc);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long documentId) {
        return find("documentId = ?1 AND deletedAt IS NOT NULL", documentId).firstResult()
                .chain(doc -> {
                    if (doc != null) {
                        return delete(doc).map(v -> true);
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
