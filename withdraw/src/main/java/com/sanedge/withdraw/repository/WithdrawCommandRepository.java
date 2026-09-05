package com.sanedge.withdraw.repository;

import com.sanedge.withdraw.entity.Withdraw;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WithdrawCommandRepository implements PanacheRepository<Withdraw> {

    @WithTransaction
    public Uni<Withdraw> markCompensationRequired(Long withdrawId, String reason) {
        return find("withdrawId = ?1 AND deletedAt IS NULL", withdrawId).firstResult()
                .chain(withdraw -> {
                    if (withdraw == null) {
                        return Uni.createFrom().nullItem();
                    }
                    withdraw.status = com.sanedge.common.enums.Status.COMPENSATION_REQUIRED;
                    withdraw.compensationRequiredAt = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                    withdraw.compensationAttempts = withdraw.compensationAttempts == null ? 1 : withdraw.compensationAttempts + 1;
                    withdraw.lastFailureReason = reason == null ? "unknown failure" : reason.substring(0, Math.min(500, reason.length()));
                    withdraw.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    return persist(withdraw).map(v -> withdraw);
                });
    }

    /** Claims a compensation record with an expiring lease using one conditional SQL update. */
    @WithTransaction
    public Uni<Boolean> claimCompensation(Long withdrawId, String workerId, String claimToken,
            java.sql.Timestamp now, java.sql.Timestamp leaseUntil, int maxAttempts) {
        return update("compensationClaimedAt = ?1, compensationClaimedBy = ?2, compensationClaimToken = ?3, compensationLeaseUntil = ?4, "
                + "compensationNextAttemptAt = NULL, "
                + "compensationAttempts = compensationAttempts + CASE WHEN compensationClaimedAt IS NOT NULL "
                + "AND (compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1) THEN 1 ELSE 0 END "
                + "WHERE withdrawId = ?5 AND deletedAt IS NULL "
                + "AND status = ?6 AND compensationAttempts < ?7 "
                + "AND (compensationNextAttemptAt IS NULL OR compensationNextAttemptAt <= ?1) "
                + "AND (compensationClaimedAt IS NULL OR compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1)",
                now, workerId, claimToken, leaseUntil, withdrawId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED,
                maxAttempts)
                .map(count -> count == 1);
    }

    @WithTransaction
    public Uni<Boolean> releaseCompensation(Long withdrawId, String workerId, String claimToken,
            java.sql.Timestamp nextAttemptAt, String reason) {
        return update("compensationClaimedAt = NULL, compensationClaimedBy = NULL, compensationClaimToken = NULL, "
                + "compensationLeaseUntil = NULL, compensationNextAttemptAt = ?1, "
                + "compensationAttempts = compensationAttempts + 1, lastFailureReason = ?2 "
                + "WHERE withdrawId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                nextAttemptAt, reason == null ? "compensation attempt failed" : reason.substring(0, Math.min(500, reason.length())),
                withdrawId, workerId, claimToken)
                .map(count -> count == 1);
    }

    /**
     * Transitions a compensation record to the terminal FAILED state once it has
     * exhausted its allowed attempts.
     */
    @WithTransaction
    public Uni<Boolean> exhaustCompensation(Long withdrawId, int maxAttempts, String reason) {
        return update("status = ?1, compensationNextAttemptAt = NULL, lastFailureReason = ?2, updatedAt = ?3 "
                + "WHERE withdrawId = ?4 AND status = ?5 AND compensationAttempts >= ?6",
                com.sanedge.common.enums.Status.FAILED,
                reason == null ? "compensation exhausted" : reason.substring(0, Math.min(500, reason.length())),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                withdrawId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts)
                .map(count -> count == 1);
    }

    /** Fenced completion hook; the external compensation adapter remains separate. */
    @WithTransaction
    public Uni<Boolean> completeCompensation(Long withdrawId, String workerId, String claimToken) {
        return completeCompensation(withdrawId, workerId, claimToken, com.sanedge.common.enums.Status.COMPENSATED);
    }

    @WithTransaction
    public Uni<Boolean> completeCompensation(Long withdrawId, String workerId, String claimToken,
            com.sanedge.common.enums.Status targetStatus) {
        return update("status = ?1, compensationClaimedAt = NULL, compensationClaimedBy = NULL, "
                + "compensationClaimToken = NULL, compensationLeaseUntil = NULL, compensationNextAttemptAt = NULL, "
                + "updatedAt = ?2 WHERE withdrawId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                targetStatus, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                withdrawId, workerId, claimToken)
                .map(count -> count == 1);
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
        return find("withdrawId = ?1 AND deletedAt IS NOT NULL", withdrawId).firstResult()
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
