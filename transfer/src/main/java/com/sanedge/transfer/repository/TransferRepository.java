package com.sanedge.transfer.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transfer.entity.Transfer;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferRepository implements PanacheRepository<Transfer> {

    public Uni<PagedResult<Transfer>> findTransfers(String search, int page, int size) {
        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(transferFrom) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(transferTo) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transferTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transfer>> findActiveTransfers(String search, int page, int size) {
        return findTransfers(search, page, size);
    }

    public Uni<PagedResult<Transfer>> findTrashedTransfers(String search, int page, int size) {
        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(transferFrom) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(transferTo) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transferTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Transfer> findTransferById(Long id) {
        return find("transferId = ?1 AND deletedAt IS NULL", id).firstResult();
    }

    public Uni<List<Transfer>> findTransfersByCardNumber(String cardNumber) {
        return list("deletedAt IS NULL AND (transferFrom = ?1 OR transferTo = ?1)", Sort.descending("transferTime"), cardNumber);
    }

    public Uni<List<Transfer>> findTransfersBySourceCard(String cardNumber) {
        return list("deletedAt IS NULL AND transferFrom = ?1", Sort.descending("transferTime"), cardNumber);
    }

    public Uni<List<Transfer>> findTransfersByDestinationCard(String cardNumber) {
        return list("deletedAt IS NULL AND transferTo = ?1", Sort.descending("transferTime"), cardNumber);
    }

    @WithTransaction
    public Uni<Transfer> updateTransferAmount(Long transferId, Long amount) {
        return find("transferId = ?1 AND deletedAt IS NULL", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer != null) {
                        transfer.transferAmount = amount != null ? amount.intValue() : 0;
                        transfer.transferTime = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                        transfer.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(transfer).map(v -> transfer);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transfer> updateTransferStatus(Long transferId, String status) {
        return find("transferId = ?1 AND deletedAt IS NULL", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer != null) {
                        try {
                            transfer.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            transfer.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            return persist(transfer).map(v -> transfer);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(transfer);
                        }
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transfer> trashed(Long transferId) {
        return find("transferId = ?1 AND deletedAt IS NULL", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer != null) {
                        transfer.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(transfer).map(v -> transfer);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transfer> restore(Long transferId) {
        return find("transferId = ?1 AND deletedAt IS NOT NULL", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer != null) {
                        transfer.setDeletedAt(null);
                        return persist(transfer).map(v -> transfer);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long transferId) {
        return find("transferId = ?1", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer != null) {
                        return delete(transfer).map(v -> true);
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
