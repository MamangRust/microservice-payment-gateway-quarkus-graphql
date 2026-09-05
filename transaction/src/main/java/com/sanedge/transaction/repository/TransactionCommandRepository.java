package com.sanedge.transaction.repository;

import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransactionCommandRepository implements PanacheRepository<Transaction> {

    @WithTransaction
    public Uni<Transaction> markCompensationRequired(Long transactionId, String reason) {
        return find("transactionId = ?1 AND deletedAt IS NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction == null) {
                        return Uni.createFrom().nullItem();
                    }
                    transaction.status = com.sanedge.common.enums.Status.COMPENSATION_REQUIRED;
                    transaction.compensationRequiredAt = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
                    transaction.compensationAttempts = transaction.compensationAttempts == null ? 1 : transaction.compensationAttempts + 1;
                    transaction.lastFailureReason = reason == null ? "unknown failure" : reason.substring(0, Math.min(500, reason.length()));
                    transaction.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                    return persist(transaction).map(v -> transaction);
                });
    }

    /** Claims a compensation record with an expiring lease using one conditional SQL update. */
    @WithTransaction
    public Uni<Boolean> claimCompensation(Long transactionId, String workerId, String claimToken,
            java.sql.Timestamp now, java.sql.Timestamp leaseUntil, int maxAttempts) {
        return update("compensationClaimedAt = ?1, compensationClaimedBy = ?2, compensationClaimToken = ?3, compensationLeaseUntil = ?4, "
                + "compensationNextAttemptAt = NULL, "
                + "compensationAttempts = compensationAttempts + CASE WHEN compensationClaimedAt IS NOT NULL "
                + "AND (compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1) THEN 1 ELSE 0 END "
                + "WHERE transactionId = ?5 AND deletedAt IS NULL "
                + "AND status = ?6 AND compensationAttempts < ?7 "
                + "AND (compensationNextAttemptAt IS NULL OR compensationNextAttemptAt <= ?1) "
                + "AND (compensationClaimedAt IS NULL OR compensationLeaseUntil IS NULL OR compensationLeaseUntil < ?1)",
                now, workerId, claimToken, leaseUntil, transactionId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED,
                maxAttempts)
                .map(count -> count == 1);
    }

    @WithTransaction
    public Uni<Boolean> releaseCompensation(Long transactionId, String workerId, String claimToken,
            java.sql.Timestamp nextAttemptAt, String reason) {
        return update("compensationClaimedAt = NULL, compensationClaimedBy = NULL, compensationClaimToken = NULL, "
                + "compensationLeaseUntil = NULL, compensationNextAttemptAt = ?1, "
                + "compensationAttempts = compensationAttempts + 1, lastFailureReason = ?2 "
                + "WHERE transactionId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                nextAttemptAt, reason == null ? "compensation attempt failed" : reason.substring(0, Math.min(500, reason.length())),
                transactionId, workerId, claimToken)
                .map(count -> count == 1);
    }

    /**
     * Transitions a compensation record to the terminal FAILED state once it has
     * exhausted its allowed attempts.
     */
    @WithTransaction
    public Uni<Boolean> exhaustCompensation(Long transactionId, int maxAttempts, String reason) {
        return update("status = ?1, compensationNextAttemptAt = NULL, lastFailureReason = ?2, updatedAt = ?3 "
                + "WHERE transactionId = ?4 AND status = ?5 AND compensationAttempts >= ?6",
                com.sanedge.common.enums.Status.FAILED,
                reason == null ? "compensation exhausted" : reason.substring(0, Math.min(500, reason.length())),
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                transactionId, com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts)
                .map(count -> count == 1);
    }

    /** Fenced completion hook; the external compensation adapter remains separate. */
    @WithTransaction
    public Uni<Boolean> completeCompensation(Long transactionId, String workerId, String claimToken) {
        return completeCompensation(transactionId, workerId, claimToken, com.sanedge.common.enums.Status.COMPENSATED);
    }

    @WithTransaction
    public Uni<Boolean> completeCompensation(Long transactionId, String workerId, String claimToken,
            com.sanedge.common.enums.Status targetStatus) {
        return update("status = ?1, compensationClaimedAt = NULL, compensationClaimedBy = NULL, "
                + "compensationClaimToken = NULL, compensationLeaseUntil = NULL, compensationNextAttemptAt = NULL, "
                + "updatedAt = ?2 WHERE transactionId = ?3 AND compensationClaimedBy = ?4 AND compensationClaimToken = ?5",
                targetStatus, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                transactionId, workerId, claimToken)
                .map(count -> count == 1);
    }

    @WithTransaction
    public Uni<Transaction> updateTransactionStatus(Long transactionId, String status) {
        return find("transactionId = ?1 AND deletedAt IS NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        try {
                            transaction.status = com.sanedge.common.enums.Status.valueOf(status.toUpperCase());
                            transaction.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            return persist(transaction).map(v -> transaction);
                        } catch (IllegalArgumentException e) {
                            return Uni.createFrom().item(transaction);
                        }
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transaction> trashed(Long transactionId) {
        return find("transactionId = ?1 AND deletedAt IS NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        transaction.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Transaction> restore(Long transactionId) {
        return find("transactionId = ?1 AND deletedAt IS NOT NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        transaction.setDeletedAt(null);
                        return persist(transaction).map(v -> transaction);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long transactionId) {
        return find("transactionId = ?1 AND deletedAt IS NOT NULL", transactionId).firstResult()
                .chain(transaction -> {
                    if (transaction != null) {
                        return delete(transaction).map(v -> true);
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
