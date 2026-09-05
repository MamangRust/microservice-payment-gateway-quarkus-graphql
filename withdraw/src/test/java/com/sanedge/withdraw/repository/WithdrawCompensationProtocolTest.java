package com.sanedge.withdraw.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.withdraw.entity.Withdraw;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Executable proof of the durable reconciliation claim protocol: exclusive
 * claims, lease expiry reclamation, stale-worker fencing, retry exhaustion to a
 * terminal state, and candidate filtering. Each step runs in its own fresh
 * session/transaction so bulk claim updates are never masked by the persistence
 * context cache.
 */
@QuarkusTestResource(PostgreSqlResource.class)
@QuarkusTest
@RunOnVertxContext
class WithdrawCompensationProtocolTest {

    @Inject
    WithdrawQueryRepository withdrawQueryRepo;

    @Inject
    WithdrawCommandRepository withdrawCommandRepo;

    private Uni<Long> persistPending(String cardNumber) {
        Withdraw w = new Withdraw();
        w.setWithdrawNo(UUID.randomUUID());
        w.setCardNumber(cardNumber);
        w.setWithdrawAmount(100000);
        w.setStatus(Status.PENDING);
        w.setWithdrawTime(Timestamp.valueOf(LocalDateTime.now()));
        w.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        w.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return Panache.withTransaction(() -> withdrawQueryRepo.persist(w)).map(Withdraw::getWithdrawId);
    }

    @Test
    void claimIsExclusiveAndCompletionIsFenced(UniAsserter asserter) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));

        asserter.execute(() -> persistPending("PROTO-CARD-01")
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id)))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-a", "token-a", now, leaseUntil, 3))
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                // second worker must not claim while lease is valid
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-b", "token-b", now, leaseUntil, 3))
                        .invoke(claimed -> assertThat(claimed).isFalse())
                        .replaceWith(id))
                // wrong token must not complete the record
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.completeCompensation(id, "worker-a", "wrong-token"))
                        .invoke(completed -> assertThat(completed).isFalse())
                        .replaceWith(id))
                // current lease owner can complete to terminal COMPENSATED
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.completeCompensation(id, "worker-a", "token-a"))
                        .invoke(completed -> assertThat(completed).isTrue())
                        .replaceWith(id))
                .chain(id -> Panache.withSession(() -> withdrawQueryRepo.findById(id)))
                .invoke(record -> {
                    assertThat(record.getStatus()).isEqualTo(Status.COMPENSATED);
                    assertThat(record.getCompensationClaimToken()).isNull();
                })
                .replaceWithVoid());
    }

    @Test
    void expiredLeaseCanBeReclaimedAndAttemptsIncrement(UniAsserter asserter) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp expiredLease = Timestamp.valueOf(LocalDateTime.now().minusMinutes(1));
        Timestamp freshLease = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));

        asserter.execute(() -> persistPending("PROTO-CARD-02")
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id)))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-a", "token-a", now, expiredLease, 5))
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                // worker-b steals the expired lease
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-b", "token-b", now, freshLease, 5))
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                .chain(id -> Panache.withSession(() -> withdrawQueryRepo.findById(id)))
                .invoke(record -> {
                    assertThat(record.getCompensationClaimedBy()).isEqualTo("worker-b");
                    // markCompensationRequired set attempts=1; the lease-steal claim after
                    // expiry increments the counter once more.
                    assertThat(record.getCompensationAttempts()).isEqualTo(2);
                })
                .replaceWithVoid());
    }

    @Test
    void retryExhaustionTransitionsToTerminalFailed(UniAsserter asserter) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));

        asserter.execute(() -> persistPending("PROTO-CARD-03")
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id)))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-a", "token-a", now, leaseUntil, 2))
                        .replaceWith(id))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo.releaseCompensation(id, "worker-a",
                        "token-a", Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), "still failing"))
                        .replaceWith(id))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo
                        .claimCompensation(id, "worker-a", "token-c", now, leaseUntil, 2))
                        .replaceWith(id))
                .chain(id -> Panache.withTransaction(() -> withdrawCommandRepo.releaseCompensation(id, "worker-a",
                        "token-c", Timestamp.valueOf(LocalDateTime.now().plusSeconds(60)), "still failing"))
                        .replaceWith(id))
                // attempts reached the cap; exhaustion must flip status to FAILED
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.exhaustCompensation(id, 2, "compensation exhausted"))
                        .invoke(exhausted -> assertThat(exhausted).isTrue())
                        .replaceWith(id))
                .chain(id -> Panache.withSession(() -> withdrawQueryRepo.findById(id)))
                .invoke(record -> {
                    assertThat(record.getStatus()).isEqualTo(Status.FAILED);
                    assertThat(record.getLastFailureReason()).contains("exhausted");
                })
                .replaceWithVoid());
    }

    @Test
    void pendingCandidatesExcludeExhaustedAndTerminalRecords(UniAsserter asserter) {
        asserter.execute(() -> persistPending("PROTO-CARD-04")
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.markCompensationRequired(id, "candidate 1").replaceWith(id)))
                .chain(v -> persistPending("PROTO-CARD-05"))
                .chain(id -> Panache.withTransaction(
                        () -> withdrawCommandRepo.markCompensationRequired(id, "candidate 2"))
                        .replaceWith(id)
                        .chain(v -> Panache.withTransaction(
                                () -> withdrawCommandRepo.exhaustCompensation(id, 1, "no more attempts"))))
                .chain(v -> persistPending("PROTO-CARD-06").replaceWithVoid())
                .chain(v -> Panache.withSession(() -> withdrawQueryRepo.findPendingCompensation(3)))
                .invoke(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).getCardNumber()).isEqualTo("PROTO-CARD-04");
                })
                .replaceWithVoid());
    }
}
