package com.sanedge.saldo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.saldo.domain.requests.FindAllSaldos;
import com.sanedge.saldo.entity.Saldo;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class SaldoRepositoryTest {

    @Inject
    SaldoQueryRepository saldoQueryRepo;

    @Inject
    SaldoCommandRepository saldoCommandRepo;

    // Helper to persist a Saldo for testing
    private Uni<Long> persist(String cardNumber, Long totalBalance, Long withdrawAmount, Timestamp withdrawTime) {
        Saldo saldo = new Saldo();
        saldo.setCardNumber(cardNumber);
        saldo.setTotalBalance(totalBalance != null ? totalBalance.intValue() : 0);
        saldo.setWithdrawAmount(withdrawAmount != null ? withdrawAmount.intValue() : 0);
        saldo.setWithdrawTime(withdrawTime);
        saldo.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        saldo.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return saldoQueryRepo.persist(saldo).map(s -> s.getSaldoId());
    }

    // Default persist with zero withdraw
    private Uni<Long> persist(String cardNumber, Long totalBalance) {
        return persist(cardNumber, totalBalance, 0L, null);
    }

    // Clean up all saldo records
    private Uni<Void> clean() {
        return saldoQueryRepo.deleteAll().replaceWithVoid();
    }

    // Build a FindAllSaldos request object
    private FindAllSaldos findAllReq(int page, int size, String search) {
        FindAllSaldos r = new FindAllSaldos();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    // ==================== Basic CRUD Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("111122223333", 150000L))
                .chain(id -> saldoQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getCardNumber()).isEqualTo("111122223333");
                    assertThat((long) found.getTotalBalance()).isEqualTo(150000L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> saldoQueryRepo.findById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumber() {
        return clean()
                .chain(() -> persist("CARD-001", 200000L))
                .chain(() -> saldoQueryRepo.findByCardNumber("CARD-001"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getCardNumber()).isEqualTo("CARD-001");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumberReturnsNullIfNotFound() {
        return clean()
                .chain(() -> saldoQueryRepo.findByCardNumber("NONEXISTENT"))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashSaldo() {
        return clean()
                .chain(() -> persist("111122223344", 200000L))
                .chain(id -> saldoCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashSaldoReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("111122223355", 200000L))
                .chain(id -> saldoCommandRepo.trashed(id)
                        .chain(() -> saldoCommandRepo.trashed(id)))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashSaldoReturnsNullIfNotFound() {
        return clean()
                .chain(() -> saldoCommandRepo.trashed(99999L))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    // ==================== Restore Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreSaldo() {
        return clean()
                .chain(() -> persist("222233334444", 300000L))
                .chain(id -> saldoCommandRepo.trashed(id)
                        .chain(() -> saldoCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreSaldoReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("222233334455", 300000L))
                .chain(id -> saldoCommandRepo.restore(id))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreSaldoReturnsNullIfNotFound() {
        return clean()
                .chain(() -> saldoCommandRepo.restore(99999L))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    // ==================== Delete Permanent Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("333344445555", 400000L))
                .chain(id -> saldoCommandRepo.trashed(id)
                        .chain(() -> saldoCommandRepo.deletePermanent(id))
                        .chain(deleted -> saldoQueryRepo.findById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persist("333344445566", 400000L))
                .chain(id -> saldoCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentReturnsFalseIfNotFound() {
        return clean()
                .chain(() -> saldoCommandRepo.deletePermanent(99999L))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("444455556666", 500000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556677", 600000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556688", 700000L))
                .chain(() -> saldoCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> saldoQueryRepo.findTrashedSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("555566667777", 800000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667788", 900000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667799", 1000000L))
                .chain(() -> saldoCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667800", 100000L))
                .chain(() -> saldoCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667811", 100000L))
                .chain(() -> saldoCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    // ==================== Update Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testUpdateBalanceByCardNumber() {
        return clean()
                .chain(() -> persist("CARD-BAL-01", 100000L))
                .chain(() -> saldoCommandRepo.updateBalanceByCardNumber("CARD-BAL-01", 250000L))
                .chain(() -> saldoQueryRepo.findByCardNumber("CARD-BAL-01"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat((long) updated.getTotalBalance()).isEqualTo(250000L);
                    assertThat(updated.getUpdatedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateBalanceReturnsZeroAffectedIfCardNotFound() {
        return clean()
                .chain(() -> saldoCommandRepo.updateBalanceByCardNumber("NONEXISTENT", 100000L))
                .invoke(count -> assertThat(count).isEqualTo(0))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateWithdrawByCardNumber() {
        return clean()
                .chain(() -> persist("CARD-WITHDRAW-01", 200000L))
                .chain(() -> saldoCommandRepo.updateWithdrawByCardNumber("CARD-WITHDRAW-01", 50000L))
                .chain(() -> saldoQueryRepo.findByCardNumber("CARD-WITHDRAW-01"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat((long) updated.getWithdrawAmount()).isEqualTo(50000L);
                    assertThat(updated.getWithdrawTime()).isNotNull();
                    assertThat(updated.getUpdatedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateWithdrawReturnsZeroAffectedIfCardNotFound() {
        return clean()
                .chain(() -> saldoCommandRepo.updateWithdrawByCardNumber("NONEXISTENT", 50000L))
                .invoke(count -> assertThat(count).isEqualTo(0))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateWithdrawWithNullAmountSetsZero() {
        return clean()
                .chain(() -> persist("CARD-WITHDRAW-02", 200000L))
                .chain(() -> saldoCommandRepo.updateWithdrawByCardNumber("CARD-WITHDRAW-02", null))
                .chain(() -> saldoQueryRepo.findByCardNumber("CARD-WITHDRAW-02"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getWithdrawAmount()).isEqualTo(0);
                })
                .replaceWithVoid();
    }

    // ==================== Query - Active/Trashed Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveSaldos() {
        return clean()
                .chain(() -> persist("ACTIVE-01", 100000L))
                .chain(() -> persist("ACTIVE-02", 200000L))
                .chain(() -> saldoQueryRepo.findActiveSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedSaldos() {
        return clean()
                .chain(() -> persist("TRASH-01", 100000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("TRASH-02", 200000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> saldoQueryRepo.findTrashedSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveSaldosExcludesTrashed() {
        return clean()
                .chain(() -> persist("MIXED-01", 100000L))
                .chain(() -> persist("MIXED-02", 200000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> saldoQueryRepo.findActiveSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedSaldosOnlyShowsTrashed() {
        return clean()
                .chain(() -> persist("MIXED-03", 100000L))
                .chain(() -> persist("MIXED-04", 200000L)
                        .chain(id -> saldoCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> saldoQueryRepo.findTrashedSaldos(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("MIXED-04");
                })
                .replaceWithVoid();
    }

    // ==================== Query - Search Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosWithSearchByCardNumber() {
        return clean()
                .chain(() -> persist("SEARCH-123", 100000L))
                .chain(() -> persist("SEARCH-456", 200000L))
                .chain(() -> persist("OTHER-789", 300000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "SEARCH")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(2);
                    assertThat(r.getData().stream().allMatch(s -> s.getCardNumber().contains("SEARCH"))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosWithEmptySearchReturnsAll() {
        return clean()
                .chain(() -> persist("AAA-001", 100000L))
                .chain(() -> persist("BBB-002", 200000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosWithNullSearchReturnsAll() {
        return clean()
                .chain(() -> persist("AAA-003", 100000L))
                .chain(() -> persist("BBB-004", 200000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, null)))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    // ==================== Query - Pagination Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosWithPagination() {
        return clean()
                .chain(() -> persist("PAGE-01", 100000L))
                .chain(() -> persist("PAGE-02", 200000L))
                .chain(() -> persist("PAGE-03", 300000L))
                .chain(() -> persist("PAGE-04", 400000L))
                .chain(() -> persist("PAGE-05", 500000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 2, "")))
                .invoke(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                })
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosPageZeroDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("PAGE-ZERO", 100000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(0, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosNegativePageDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("PAGE-NEG", 100000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(-1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosZeroSizeDefaultsToTen() {
        return clean()
                .chain(() -> persist("SIZE-ZERO", 100000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 0, "")))
                .invoke(r -> assertThat(r.getData()).hasSizeLessThanOrEqualTo(10))
                .replaceWithVoid();
    }

    // ==================== Query - Sort Order Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindSaldosSortedBySaldoIdAscending() {
        // saldoId is auto-generated; we persist in order and expect ascending IDs
        return clean()
                .chain(() -> persist("SORT-01", 100000L))
                .chain(() -> persist("SORT-02", 200000L))
                .chain(() -> persist("SORT-03", 300000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(3);
                    // since IDs are assigned sequentially, first should have smallest ID
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("SORT-01");
                    assertThat(r.getData().get(1).getCardNumber()).isEqualTo("SORT-02");
                    assertThat(r.getData().get(2).getCardNumber()).isEqualTo("SORT-03");
                })
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> saldoQueryRepo.findActiveSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> saldoQueryRepo.findTrashedSaldos(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchWithNoMatchesReturnsZeroRecords() {
        return clean()
                .chain(() -> persist("UNIQUE-001", 100000L))
                .chain(() -> saldoQueryRepo.findSaldos(findAllReq(1, 10, "NONEXISTENT")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashThenFindByCardNumberReturnsNull() {
        return clean()
                .chain(() -> persist("TRASH-CARD", 100000L)
                        .chain(id -> saldoCommandRepo.trashed(id)))
                .chain(() -> saldoQueryRepo.findByCardNumber("TRASH-CARD"))
                .invoke(found -> assertThat(found).isNull()) // because deletedAt is set
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testBalanceUpdateIgnoresTrashedSaldo() {
        return clean()
                .chain(() -> persist("TRASH-BAL", 500000L))
                .chain(id -> saldoCommandRepo.trashed(id))
                .chain(() -> saldoCommandRepo.updateBalanceByCardNumber("TRASH-BAL", 999999L))
                .invoke(affected -> assertThat(affected).isEqualTo(0))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testWithdrawUpdateIgnoresTrashedSaldo() {
        return clean()
                .chain(() -> persist("TRASH-WITH", 500000L))
                .chain(id -> saldoCommandRepo.trashed(id))
                .chain(() -> saldoCommandRepo.updateWithdrawByCardNumber("TRASH-WITH", 50000L))
                .invoke(affected -> assertThat(affected).isEqualTo(0))
                .replaceWithVoid();
    }
}