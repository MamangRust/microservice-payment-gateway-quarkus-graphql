package com.sanedge.topup.repository;

import com.sanedge.topup.entity.Topup;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TopupCommandRepository implements PanacheRepository<Topup> {

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
    public Uni<Topup> markCompensationRequired(Long topupId, String reason) {
        return find("topupId = ?1 AND deletedAt IS NULL", topupId).firstResult()
                .chain(topup -> {
                    if (topup == null) {
                        return Uni.createFrom().nullItem();
                    }
                    topup.status = com.sanedge.common.enums.Status.COMPENSATION_REQUIRED;
                    topup.compensationRequiredAt = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                    topup.compensationAttempts = topup.compensationAttempts == null ? 1 : topup.compensationAttempts + 1;
                    topup.lastFailureReason = reason == null ? "unknown failure" : reason.substring(0, Math.min(500, reason.length()));
                    topup.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    return persist(topup).map(v -> topup);
                });
    }

    /**
     * Claims one known compensation record with a lease. The conditional update is
     * the concurrency boundary: only one worker can win while the lease is valid.
     */
    @WithTransaction
    public Uni<Boolean> claimCompensation(Long topupId, String workerId, String claimToken,
            java.sql.Timestamp now, java.sql.Timestamp leaseUntil, int maxAttempts) {
        return update("compensationClaimedAt = ?1, compensationClaimedBy = ?2, compensationClaimToken = ?3, compensationLeaseUntil = ?4, "
                + "compensationNextAttemptAt = NULL, "
                + "compensationAttempts = compensationAttempts + CASE WHEN compensationClaimedAt IS NOT NULL "
                + "AND (compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1) THEN 1 ELSE 0 END "
                + "WHERE topupId = ?5 AND deletedAt IS NULL "
                + "AND status = ?6 AND compensationAttempts < ?7 "
                + "AND (compensationNextAttemptAt IS NULL OR compensationNextAttemptAt <= ?1) "
                + "AND (compensationClaimedAt IS NULL OR compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1)",
                now, workerId, claimToken, leaseUntil, topupId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED,
                maxAttempts)
                .map(count -> count == 1);
    }

    @WithTransaction
    public Uni<Boolean> releaseCompensation(Long topupId, String workerId, String claimToken,
            java.sql.Timestamp nextAttemptAt, String reason) {
        return update("compensationClaimedAt = NULL, compensationClaimedBy = NULL, compensationClaimToken = NULL, "
                + "compensationLeaseUntil = NULL, compensationNextAttemptAt = ?1, "
                + "compensationAttempts = compensationAttempts + 1, lastFailureReason = ?2 "
                + "WHERE topupId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                nextAttemptAt, reason == null ? "compensation attempt failed" : reason.substring(0, Math.min(500, reason.length())),
                topupId, workerId, claimToken)
                .map(count -> count == 1);
    }

    /**
     * Transitions a compensation record to the terminal FAILED state once it has
     * exhausted its allowed attempts. Runs after releaseCompensation has already
     * incremented the attempt counter, so records with compensationAttempts >= max
     * are no longer picked up by findPendingCompensation.
     */
    @WithTransaction
    public Uni<Boolean> exhaustCompensation(Long topupId, int maxAttempts, String reason) {
        return update("status = ?1, compensationNextAttemptAt = NULL, lastFailureReason = ?2, updatedAt = ?3 "
                + "WHERE topupId = ?4 AND status = ?5 AND compensationAttempts >= ?6",
                com.sanedge.common.enums.Status.FAILED,
                reason == null ? "compensation exhausted" : reason.substring(0, Math.min(500, reason.length())),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                topupId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts)
                .map(count -> count == 1);
    }

    /**
     * Fenced completion hook for a real compensation adapter. It can only be called
     * by the current lease owner and never performs the external rollback itself.
     */
    @WithTransaction
    public Uni<Boolean> completeCompensation(Long topupId, String workerId, String claimToken) {
        return completeCompensation(topupId, workerId, claimToken, com.sanedge.common.enums.Status.COMPENSATED);
    }

    @WithTransaction
    public Uni<Boolean> completeCompensation(Long topupId, String workerId, String claimToken,
            com.sanedge.common.enums.Status targetStatus) {
        return update("status = ?1, compensationClaimedAt = NULL, compensationClaimedBy = NULL, "
                + "compensationClaimToken = NULL, compensationLeaseUntil = NULL, compensationNextAttemptAt = NULL, "
                + "updatedAt = ?2 WHERE topupId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                targetStatus, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                topupId, workerId, claimToken)
                .map(count -> count == 1);
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
        return find("topupId = ?1 AND deletedAt IS NOT NULL", topupId).firstResult()
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
