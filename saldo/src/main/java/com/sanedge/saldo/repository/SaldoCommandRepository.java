package com.sanedge.saldo.repository;

import com.sanedge.saldo.entity.Saldo;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoCommandRepository implements PanacheRepository<Saldo> {

    @WithTransaction
    public Uni<Void> lockCardForCreate(String cardNumber) {
        return getSession()
                .chain(session -> session.createNativeQuery(
                                "SELECT pg_advisory_xact_lock(hashtext(:key))", Object.class)
                        .setParameter("key", "saldo-create:" + cardNumber)
                        .getSingleResult())
                .replaceWithVoid();
    }

    @WithTransaction
    public Uni<Saldo> trashed(Long saldoId) {
        return find("saldoId = ?1 AND deletedAt IS NULL", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        saldo.setDeletedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        return persist(saldo).map(v -> saldo);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Saldo> restore(Long saldoId) {
        return find("saldoId = ?1 AND deletedAt IS NOT NULL", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        saldo.setDeletedAt(null);
                        return persist(saldo).map(v -> saldo);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Integer> updateBalanceByCardNumber(String cardNumber, Long newBalance) {
        return update("totalBalance = ?1, updatedAt = ?2 WHERE cardNumber = ?3 AND deletedAt IS NULL",
                newBalance != null ? newBalance.intValue() : 0,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()),
                cardNumber);
    }

    /**
     * Atomically applies a balance delta and refuses a debit that would cross the
     * supplied minimum. This avoids the read-modify-write race between financial
     * services that share a card balance.
     */
    @WithTransaction
    public Uni<Integer> updateBalanceByDelta(String cardNumber, Long delta, Long minimumBalance) {
        return updateBalanceByDelta(cardNumber, delta, minimumBalance, null);
    }

    /**
     * Applies one saldo delta exactly once for operationKey. The operation row is
     * inserted under the same transaction as the guarded balance update; a replay
     * returns the recorded APPLIED result without applying the delta again. A
     * previously REJECTED or crashed PROCESSING operation can be retried by the
     * next attempt, so transient dependency failures recover without double
     * applying a successful mutation.
     */
    @WithTransaction
    public Uni<Integer> updateBalanceByDelta(String cardNumber, Long delta, Long minimumBalance,
            String operationKey) {
        int appliedDelta = delta == null ? 0 : delta.intValue();
        int minimum = minimumBalance == null ? 0 : minimumBalance.intValue();
        if (operationKey == null || operationKey.isBlank()) {
            return update("totalBalance = totalBalance + ?1, updatedAt = ?2 "
                    + "WHERE cardNumber = ?3 AND deletedAt IS NULL AND totalBalance + ?1 >= ?4",
                    appliedDelta, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), cardNumber, minimum);
        }
        return getSession().chain(session -> session.createNativeQuery(
                        "INSERT INTO saldo_mutation_operations "
                                + "(operation_key, card_number, requested_delta, minimum_balance, result_status, created_at, updated_at) "
                                + "VALUES (:key, :card, :delta, :minimum, 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                                + "ON CONFLICT (operation_key) DO NOTHING")
                .setParameter("key", operationKey).setParameter("card", cardNumber)
                .setParameter("delta", appliedDelta).setParameter("minimum", minimum).executeUpdate())
                .chain(inserted -> {
                    if (inserted == 1) {
                        return applyBalanceDelta(cardNumber, appliedDelta, minimum, operationKey);
                    }
                    return claimOperationRetry(operationKey)
                            .chain(claimed -> claimed
                                    ? applyBalanceDelta(cardNumber, appliedDelta, minimum, operationKey)
                                    : replayOperationResult(operationKey));
                });
    }

    private Uni<Integer> applyBalanceDelta(String cardNumber, int appliedDelta, int minimum, String operationKey) {
        return update("totalBalance = totalBalance + ?1, updatedAt = ?2 "
                + "WHERE cardNumber = ?3 AND deletedAt IS NULL AND totalBalance + ?1 >= ?4",
                appliedDelta, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), cardNumber, minimum)
                .chain(updated -> recordOperationResult(operationKey, updated == 1)
                        .replaceWith(updated));
    }

    /**
     * Atomically claims a previously REJECTED operation so exactly one retry
     * applies the delta. A concurrently in-flight PROCESSING operation is not
     * claimable; the caller must then consult the recorded status to decide
     * success (APPLIED) or back-off (still processing/rejected).
     */
    private Uni<Boolean> claimOperationRetry(String operationKey) {
        return getSession().chain(session -> session.createNativeQuery(
                        "UPDATE saldo_mutation_operations SET result_status = 'PROCESSING', "
                                + "updated_at = CURRENT_TIMESTAMP "
                                + "WHERE operation_key = :key AND result_status = 'REJECTED'")
                .setParameter("key", operationKey).executeUpdate())
                .map(count -> count == 1);
    }

    /**
     * Reads the terminal outcome of an already-present operation row. Returns 1
     * when APPLIED (replay success, no re-apply), otherwise 0 so the caller can
     * back off and retry later.
     */
    private Uni<Integer> replayOperationResult(String operationKey) {
        return getSession().chain(session -> session.createNativeQuery(
                        "SELECT result_status FROM saldo_mutation_operations WHERE operation_key = :key", String.class)
                .setParameter("key", operationKey).getSingleResult())
                .map(status -> "APPLIED".equals(String.valueOf(status)) ? 1 : 0);
    }

    private Uni<Integer> recordOperationResult(String operationKey, boolean applied) {
        return getSession().chain(session -> session.createNativeQuery(
                        "UPDATE saldo_mutation_operations SET result_status = :status, "
                                + "failure_reason = :reason, updated_at = CURRENT_TIMESTAMP "
                                + "WHERE operation_key = :key")
                .setParameter("status", applied ? "APPLIED" : "REJECTED")
                .setParameter("reason", applied ? null : "saldo missing or minimum balance violated")
                .setParameter("key", operationKey).executeUpdate());
    }

    @WithTransaction
    public Uni<Integer> updateBalanceAndWithdrawByDelta(String cardNumber, Long delta, Long minimumBalance,
            Long withdrawAmount, java.sql.Timestamp withdrawTime) {
        return updateBalanceAndWithdrawByDelta(cardNumber, delta, minimumBalance, withdrawAmount, withdrawTime, null);
    }

    @WithTransaction
    public Uni<Integer> updateBalanceAndWithdrawByDelta(String cardNumber, Long delta, Long minimumBalance,
            Long withdrawAmount, java.sql.Timestamp withdrawTime, String operationKey) {
        int appliedDelta = delta == null ? 0 : delta.intValue();
        int minimum = minimumBalance == null ? 0 : minimumBalance.intValue();
        java.sql.Timestamp resolvedWithdrawTime = withdrawTime == null
                ? java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()) : withdrawTime;
        if (operationKey == null || operationKey.isBlank()) {
            return update("totalBalance = totalBalance + ?1, withdrawAmount = ?2, withdrawTime = ?3, updatedAt = ?4 "
                    + "WHERE cardNumber = ?5 AND deletedAt IS NULL AND totalBalance + ?1 >= ?6",
                    appliedDelta, withdrawAmount == null ? 0 : withdrawAmount.intValue(), resolvedWithdrawTime,
                    java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), cardNumber, minimum);
        }
        return getSession().chain(session -> session.createNativeQuery(
                        "INSERT INTO saldo_mutation_operations "
                                + "(operation_key, card_number, requested_delta, minimum_balance, result_status, created_at, updated_at) "
                                + "VALUES (:key, :card, :delta, :minimum, 'PROCESSING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                                + "ON CONFLICT (operation_key) DO NOTHING")
                .setParameter("key", operationKey).setParameter("card", cardNumber)
                .setParameter("delta", appliedDelta).setParameter("minimum", minimum).executeUpdate())
                .chain(inserted -> {
                    if (inserted == 1) {
                        return applyWithdrawDelta(cardNumber, appliedDelta, minimum, withdrawAmount,
                                resolvedWithdrawTime, operationKey);
                    }
                    return claimOperationRetry(operationKey)
                            .chain(claimed -> claimed
                                    ? applyWithdrawDelta(cardNumber, appliedDelta, minimum, withdrawAmount,
                                            resolvedWithdrawTime, operationKey)
                                    : replayOperationResult(operationKey));
                });
    }

    private Uni<Integer> applyWithdrawDelta(String cardNumber, int appliedDelta, int minimum, Long withdrawAmount,
            java.sql.Timestamp withdrawTime, String operationKey) {
        return update("totalBalance = totalBalance + ?1, withdrawAmount = ?2, withdrawTime = ?3, updatedAt = ?4 "
                + "WHERE cardNumber = ?5 AND deletedAt IS NULL AND totalBalance + ?1 >= ?6",
                appliedDelta, withdrawAmount == null ? 0 : withdrawAmount.intValue(), withdrawTime,
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), cardNumber, minimum)
                .chain(updated -> recordOperationResult(operationKey, updated == 1)
                        .replaceWith(updated));
    }

    @WithTransaction
    public Uni<Integer> updateWithdrawByCardNumber(String cardNumber, Long withdrawAmount) {
        java.sql.Timestamp now = java.sql.Timestamp.valueOf(java.time.LocalDateTime.now());
        return update("withdrawAmount = ?1, withdrawTime = ?2, updatedAt = ?3 WHERE cardNumber = ?4 AND deletedAt IS NULL",
                withdrawAmount != null ? withdrawAmount.intValue() : 0,
                now,
                now,
                cardNumber);
    }

    @WithTransaction
    public Uni<Boolean> deletePermanent(Long saldoId) {
        return find("saldoId = ?1 AND deletedAt IS NOT NULL", saldoId).firstResult()
                .chain(saldo -> {
                    if (saldo != null) {
                        return delete(saldo).map(v -> true);
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
