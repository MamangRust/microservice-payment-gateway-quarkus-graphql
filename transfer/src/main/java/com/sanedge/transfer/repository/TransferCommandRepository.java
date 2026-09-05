package com.sanedge.transfer.repository;

import com.sanedge.transfer.entity.Transfer;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferCommandRepository implements PanacheRepository<Transfer> {

    @WithTransaction
    public Uni<Transfer> markCompensationRequired(Long transferId, String reason) {
        return find("transferId = ?1 AND deletedAt IS NULL", transferId).firstResult()
                .chain(transfer -> {
                    if (transfer == null) {
                        return Uni.createFrom().nullItem();
                    }
                    transfer.status = com.sanedge.common.enums.Status.COMPENSATION_REQUIRED;
                    transfer.compensationRequiredAt = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                    transfer.compensationAttempts = transfer.compensationAttempts == null ? 1 : transfer.compensationAttempts + 1;
                    transfer.lastFailureReason = reason == null ? "unknown failure" : reason.substring(0, Math.min(500, reason.length()));
                    transfer.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    return persist(transfer).map(v -> transfer);
                });
    }

    /** Claims a compensation record with an expiring lease using one conditional SQL update. */
    @WithTransaction
    public Uni<Boolean> claimCompensation(Long transferId, String workerId, String claimToken,
            java.sql.Timestamp now, java.sql.Timestamp leaseUntil, int maxAttempts) {
        return update("compensationClaimedAt = ?1, compensationClaimedBy = ?2, compensationClaimToken = ?3, compensationLeaseUntil = ?4, "
                + "compensationNextAttemptAt = NULL, "
                + "compensationAttempts = compensationAttempts + CASE WHEN compensationClaimedAt IS NOT NULL "
                + "AND (compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1) THEN 1 ELSE 0 END "
                + "WHERE transferId = ?5 AND deletedAt IS NULL "
                + "AND status = ?6 AND compensationAttempts < ?7 "
                + "AND (compensationNextAttemptAt IS NULL OR compensationNextAttemptAt <= ?1) "
                + "AND (compensationClaimedAt IS NULL OR compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1)",
                now, workerId, claimToken, leaseUntil, transferId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED,
                maxAttempts)
                .map(count -> count == 1);
    }

    @WithTransaction
    public Uni<Boolean> releaseCompensation(Long transferId, String workerId, String claimToken,
            java.sql.Timestamp nextAttemptAt, String reason) {
        return update("compensationClaimedAt = NULL, compensationClaimedBy = NULL, compensationClaimToken = NULL, "
                + "compensationLeaseUntil = NULL, compensationNextAttemptAt = ?1, "
                + "compensationAttempts = compensationAttempts + 1, lastFailureReason = ?2 "
                + "WHERE transferId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                nextAttemptAt, reason == null ? "compensation attempt failed" : reason.substring(0, Math.min(500, reason.length())),
                transferId, workerId, claimToken)
                .map(count -> count == 1);
    }

    /**
     * Transitions a compensation record to the terminal FAILED state once it has
     * exhausted its allowed attempts.
     */
    @WithTransaction
    public Uni<Boolean> exhaustCompensation(Long transferId, int maxAttempts, String reason) {
        return update("status = ?1, compensationNextAttemptAt = NULL, lastFailureReason = ?2, updatedAt = ?3 "
                + "WHERE transferId = ?4 AND status = ?5 AND compensationAttempts >= ?6",
                com.sanedge.common.enums.Status.FAILED,
                reason == null ? "compensation exhausted" : reason.substring(0, Math.min(500, reason.length())),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                transferId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts)
                .map(count -> count == 1);
    }

    /** Fenced completion hook; the external compensation adapter remains separate. */
    @WithTransaction
    public Uni<Boolean> completeCompensation(Long transferId, String workerId, String claimToken) {
        return completeCompensation(transferId, workerId, claimToken, com.sanedge.common.enums.Status.COMPENSATED);
    }

    @WithTransaction
    public Uni<Boolean> completeCompensation(Long transferId, String workerId, String claimToken,
            com.sanedge.common.enums.Status targetStatus) {
        return update("status = ?1, compensationClaimedAt = NULL, compensationClaimedBy = NULL, "
                + "compensationClaimToken = NULL, compensationLeaseUntil = NULL, compensationNextAttemptAt = NULL, "
                + "updatedAt = ?2 WHERE transferId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                targetStatus, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                transferId, workerId, claimToken)
                .map(count -> count == 1);
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
        return find("transferId = ?1 AND deletedAt IS NOT NULL", transferId).firstResult()
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
