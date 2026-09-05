package com.sanedge.saldo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;
import com.sanedge.saldo.entity.Saldo;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Executable proof that the saldo mutation operation ledger is exactly-once:
 * replaying an operation key returns the original APPLIED outcome without
 * re-applying the delta, while a previously REJECTED operation can be retried
 * once the balance permits. Each step uses a fresh session/transaction so bulk
 * balance updates are read from the database rather than the persistence
 * context cache.
 */
@QuarkusTestResource(PostgreSqlResource.class)
@QuarkusTest
@RunOnVertxContext
class SaldoMutationOperationRepositoryTest {

    @Inject
    SaldoQueryRepository saldoQueryRepo;

    @Inject
    SaldoCommandRepository saldoCommandRepo;

    private Uni<Long> persistSaldo(String cardNumber, long balance) {
        Saldo saldo = new Saldo();
        saldo.setCardNumber(cardNumber);
        saldo.setTotalBalance((int) balance);
        saldo.setWithdrawAmount(0);
        saldo.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
        saldo.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
        return Panache.withTransaction(() -> saldoQueryRepo.persist(saldo)).map(Saldo::getSaldoId);
    }

    private Uni<Void> clean() {
        return Panache.withTransaction(() -> saldoQueryRepo.getSession()
                .chain(session -> session.createNativeQuery("TRUNCATE TABLE saldo_mutation_operations")
                        .executeUpdate())
                .chain(v -> saldoQueryRepo.deleteAll().replaceWithVoid()));
    }

    @Test
    void deltaAppliesExactlyOnceForDeterministicOperationKey(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> persistSaldo("CARD-OP-1", 100000L))
                .chain(id -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-1", -25000L, 0L, "op:test:1")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                .chain(() -> Panache.withSession(() -> saldoQueryRepo.findByCardNumber("CARD-OP-1")))
                .invoke(saldo -> assertThat((long) saldo.getTotalBalance()).isEqualTo(75000L))
                // Replay the same operation key: outcome must not be re-applied.
                .chain(() -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-1", -25000L, 0L, "op:test:1")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                .chain(() -> Panache.withSession(() -> saldoQueryRepo.findByCardNumber("CARD-OP-1")))
                .invoke(saldo -> assertThat((long) saldo.getTotalBalance()).isEqualTo(75000L))
                .replaceWithVoid());
    }

    @Test
    void rejectedOperationCanBeRetriedAfterBalanceImproves(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> persistSaldo("CARD-OP-2", 1000L))
                // Debit beyond available balance: operation must be rejected, not applied.
                .chain(id -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-2", -5000L, 0L, "op:test:2")))
                .invoke(updated -> assertThat(updated).isZero())
                // Top up the balance so the previously rejected operation can succeed.
                .chain(() -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-2", 6000L, 0L, "op:test:2x")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                // Retry the rejected operation key: it must now be re-attempted and apply.
                .chain(() -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-2", -5000L, 0L, "op:test:2")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                .chain(() -> Panache.withSession(() -> saldoQueryRepo.findByCardNumber("CARD-OP-2")))
                .invoke(saldo -> assertThat((long) saldo.getTotalBalance()).isEqualTo(2000L))
                .replaceWithVoid());
    }

    @Test
    void appliedReplayReturnsSuccessEvenWithDifferentDeltaValue(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> persistSaldo("CARD-OP-3", 50000L))
                .chain(id -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-3", -10000L, 0L, "op:test:3")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                // A stale retry with the same key but a different delta must not corrupt.
                .chain(() -> Panache.withTransaction(
                        () -> saldoCommandRepo.updateBalanceByDelta("CARD-OP-3", -99999L, 0L, "op:test:3")))
                .invoke(updated -> assertThat(updated).isEqualTo(1))
                .chain(() -> Panache.withSession(() -> saldoQueryRepo.findByCardNumber("CARD-OP-3")))
                .invoke(saldo -> assertThat((long) saldo.getTotalBalance()).isEqualTo(40000L))
                .replaceWithVoid());
    }
}
