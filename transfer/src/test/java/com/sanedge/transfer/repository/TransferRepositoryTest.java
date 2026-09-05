package com.sanedge.transfer.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.transfer.domain.requests.FindAllTransfers;
import com.sanedge.transfer.entity.Transfer;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class TransferRepositoryTest {

    @Inject
    TransferQueryRepository transferQueryRepo;

    @Inject
    TransferCommandRepository transferCommandRepo;

    private Uni<Long> persist(String from, String to, Long amount, Status status, LocalDateTime transferTime) {
        Transfer transfer = new Transfer();
        transfer.setTransferFrom(from);
        transfer.setTransferTo(to);
        transfer.setTransferAmount(amount != null ? amount.intValue() : 0);
        transfer.setStatus(status);
        if (transferTime != null) {
            transfer.setTransferTime(Timestamp.valueOf(transferTime));
        }
        transfer.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        transfer.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return transferQueryRepo.persist(transfer).map(t -> t.getTransferId());
    }

    private Uni<Long> persist(String from, String to, Long amount) {
        return persist(from, to, amount, Status.PENDING, null);
    }

    private Uni<Void> clean() {
        return transferQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllTransfers findAllReq(int page, int size, String search) {
        FindAllTransfers r = new FindAllTransfers();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("111122223333", "444455556666", 150000L))
                .chain(id -> transferQueryRepo.findTransferById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getTransferFrom()).isEqualTo("111122223333");
                    assertThat(found.getTransferTo()).isEqualTo("444455556666");
                    assertThat((long) found.getTransferAmount()).isEqualTo(150000L);
                    assertThat(found.getStatus()).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> transferQueryRepo.findTransferById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashTransfer() {
        return clean()
                .chain(() -> persist("111122223333", "444455556666", 200000L))
                .chain(id -> transferCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashTransferReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("111122223344", "444455556677", 200000L))
                .chain(id -> transferCommandRepo.trashed(id)
                        .chain(() -> transferCommandRepo.trashed(id)))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashTransferReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transferCommandRepo.trashed(99999L))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    // ==================== Restore Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransfer() {
        return clean()
                .chain(() -> persist("222233334444", "555566667777", 300000L))
                .chain(id -> transferCommandRepo.trashed(id)
                        .chain(() -> transferCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransferReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("222233334455", "555566667788", 300000L))
                .chain(id -> transferCommandRepo.restore(id))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransferReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transferCommandRepo.restore(99999L))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    // ==================== Delete Permanent Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("333344445555", "666677778888", 400000L))
                .chain(id -> transferCommandRepo.trashed(id)
                        .chain(() -> transferCommandRepo.deletePermanent(id))
                        .chain(deleted -> transferQueryRepo.findTransferById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persist("333344445566", "666677778899", 400000L))
                .chain(id -> transferCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentReturnsFalseIfNotFound() {
        return clean()
                .chain(() -> transferCommandRepo.deletePermanent(99999L))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("444455556666", "777788889999", 500000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556677", "777788889900", 600000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556688", "777788889911", 700000L))
                .chain(() -> transferCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> transferQueryRepo.findTrashedTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("555566667777", "888899990000", 800000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667788", "888899990011", 900000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667799", "888899990022", 1000000L))
                .chain(() -> transferCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667800", "888899990033", 100000L))
                .chain(() -> transferCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667811", "888899990044", 100000L))
                .chain(() -> transferCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    // ==================== Update Status Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus() {
        return clean()
                .chain(() -> persist("666677778888", "999900001111", 100000L, Status.PENDING, null))
                .chain(id -> transferCommandRepo.updateTransferStatus(id, "SUCCESS"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus()).isEqualTo(Status.SUCCESS);
                    assertThat(updated.getUpdatedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusToFailed() {
        return clean()
                .chain(() -> persist("666677778899", "999900001122", 100000L, Status.PENDING, null))
                .chain(id -> transferCommandRepo.updateTransferStatus(id, "FAILED"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus()).isEqualTo(Status.FAILED);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transferCommandRepo.updateTransferStatus(99999L, "SUCCESS"))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusInvalidStatusDoesNotChange() {
        return clean()
                .chain(() -> persist("666677778900", "999900001133", 100000L, Status.PENDING, null))
                .chain(id -> transferCommandRepo.updateTransferStatus(id, "INVALID_STATUS"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus()).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    // ==================== Update Amount Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testUpdateTransferAmount() {
        return clean()
                .chain(() -> persist("666677779000", "999900001144", 100000L, Status.PENDING, null))
                .chain(id -> transferCommandRepo.updateTransferAmount(id, 250000L))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat((long) updated.getTransferAmount()).isEqualTo(250000L);
                    assertThat(updated.getUpdatedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateTransferAmountReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transferCommandRepo.updateTransferAmount(99999L, 250000L))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    // ==================== Query - Active/Trashed Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveTransfers() {
        return clean()
                .chain(() -> persist("777788889999", "111122223333", 100000L))
                .chain(() -> persist("777788889900", "111122223344", 200000L))
                .chain(() -> transferQueryRepo.findActiveTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedTransfers() {
        return clean()
                .chain(() -> persist("888899990000", "222233334444", 100000L))
                .chain(() -> persist("888899990011", "222233334455", 200000L))
                .chain(() -> transferQueryRepo.findTrashedTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveTransfersExcludesTrashed() {
        return clean()
                .chain(() -> persist("888899990022", "222233334466", 100000L))
                .chain(() -> persist("888899990033", "222233334477", 200000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transferQueryRepo.findActiveTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedTransfersOnlyShowsTrashed() {
        return clean()
                .chain(() -> persist("888899990044", "222233334488", 100000L))
                .chain(() -> persist("888899990055", "222233334499", 200000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transferQueryRepo.findTrashedTransfers(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getTransferFrom()).isEqualTo("888899990055");
                })
                .replaceWithVoid();
    }

    // ==================== Query - Search Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithSearchByTransferFrom() {
        return clean()
                .chain(() -> persist("1234567890123", "999999999999", 100000L))
                .chain(() -> persist("9876543210987", "888888888888", 200000L))
                .chain(() -> persist("1111222233333", "777777777777", 300000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "1234")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getTransferFrom()).isEqualTo("1234567890123");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithSearchByTransferTo() {
        return clean()
                .chain(() -> persist("1234567890123", "TARGET_CARD_1", 100000L))
                .chain(() -> persist("9876543210987", "TARGET_CARD_2", 200000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "TARGET_CARD_1")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getTransferTo()).isEqualTo("TARGET_CARD_1");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithSearchByStatus() {
        return clean()
                .chain(() -> persist("1234567890123", "999999999999", 100000L, Status.SUCCESS, null))
                .chain(() -> persist("9876543210987", "888888888888", 200000L, Status.FAILED, null))
                .chain(() -> persist("1111222233333", "777777777777", 300000L, Status.PENDING, null))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "success")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getStatus()).isEqualTo(Status.SUCCESS);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithEmptySearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123", "999999999999", 100000L))
                .chain(() -> persist("9876543210987", "888888888888", 200000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithNullSearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123", "999999999999", 100000L))
                .chain(() -> persist("9876543210987", "888888888888", 200000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, null)))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    // ==================== Query - Pagination Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersWithPagination() {
        return clean()
                .chain(() -> persist("1111111111001", "2222222222001", 100000L))
                .chain(() -> persist("1111111111002", "2222222222002", 200000L))
                .chain(() -> persist("1111111111003", "2222222222003", 300000L))
                .chain(() -> persist("1111111111004", "2222222222004", 400000L))
                .chain(() -> persist("1111111111005", "2222222222005", 500000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 2, "")))
                .map(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                    return page1;
                })
                .chain(page1 -> transferQueryRepo.findTransfers(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersPageZeroDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("1111111111006", "2222222222006", 100000L))
                .chain(() -> persist("1111111111007", "2222222222007", 200000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(0, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersNegativePageDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("1111111111008", "2222222222008", 100000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(-1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersZeroSizeDefaultsToTen() {
        return clean()
                .chain(() -> persist("1111111111009", "2222222222009", 100000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 0, "")))
                .invoke(r -> assertThat(r.getData()).hasSizeLessThanOrEqualTo(10))
                .replaceWithVoid();
    }

    // ==================== Query - Sort Order Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersSortedByTransferTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 3, 10, 0, 0);

        return clean()
                .chain(() -> persist("1111111111010", "2222222222010", 100000L, Status.PENDING, time1))
                .chain(() -> persist("1111111111011", "2222222222011", 200000L, Status.PENDING, time2))
                .chain(() -> persist("1111111111012", "2222222222012", 300000L, Status.PENDING, time3))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(3);
                    assertThat(r.getData().get(0).getTransferTime()).isEqualTo(time3);
                    assertThat(r.getData().get(1).getTransferTime()).isEqualTo(time2);
                    assertThat(r.getData().get(2).getTransferTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    // ==================== Query - By Card Number Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersByCardNumber() {
        return clean()
                .chain(() -> persist("123412341234", "999999999999", 100000L))
                .chain(() -> persist("999999999999", "123412341234", 200000L))
                .chain(() -> persist("555555555555", "666666666666", 300000L))
                .chain(() -> transferQueryRepo.findTransfersByCardNumber("123412341234"))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().allMatch(t -> t.getTransferFrom().equals("123412341234") ||
                            t.getTransferTo().equals("123412341234"))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersBySourceCard() {
        return clean()
                .chain(() -> persist("123412341234", "999999999999", 100000L))
                .chain(() -> persist("999999999999", "123412341234", 200000L))
                .chain(() -> transferQueryRepo.findTransfersBySourceCard("123412341234"))
                .invoke(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).getTransferFrom()).isEqualTo("123412341234");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersByDestinationCard() {
        return clean()
                .chain(() -> persist("123412341234", "999999999999", 100000L))
                .chain(() -> persist("999999999999", "123412341234", 200000L))
                .chain(() -> transferQueryRepo.findTransfersByDestinationCard("123412341234"))
                .invoke(list -> {
                    assertThat(list).hasSize(1);
                    assertThat(list.get(0).getTransferTo()).isEqualTo("123412341234");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersByCardNumberExcludesTrashed() {
        return clean()
                .chain(() -> persist("123412341234", "999999999999", 100000L))
                .chain(() -> persist("123412341234", "888888888888", 200000L)
                        .chain(id -> transferCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transferQueryRepo.findTransfersByCardNumber("123412341234"))
                .invoke(list -> assertThat(list).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransfersByCardNumberSortedByTransferTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);

        return clean()
                .chain(() -> persist("123412341235", "999999999999", 100000L, Status.PENDING, time1))
                .chain(() -> persist("123412341235", "888888888888", 200000L, Status.PENDING, time2))
                .chain(() -> transferQueryRepo.findTransfersByCardNumber("123412341235"))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0).getTransferTime()).isEqualTo(time2);
                    assertThat(list.get(1).getTransferTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> transferQueryRepo.findActiveTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> transferQueryRepo.findTrashedTransfers(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchWithNoMatchesReturnsZeroRecords() {
        return clean()
                .chain(() -> persist("1234567890123", "999999999999", 100000L))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "NONEXISTENT_SEARCH")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusOnTrashedTransferReturnsNull() {
        return clean()
                .chain(() -> persist("999988887777", "666655554444", 100000L))
                .chain(id -> transferCommandRepo.trashed(id)
                        .chain(() -> transferCommandRepo.updateTransferStatus(id, "SUCCESS")))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCaseInsensitiveStatusSearch() {
        return clean()
                .chain(() -> persist("111122223344", "555566667788", 100000L, Status.SUCCESS, null))
                .chain(() -> persist("111122223355", "555566667799", 200000L, Status.FAILED, null))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "SUCCESS")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "success")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transferQueryRepo.findTransfers(findAllReq(1, 10, "SuCceSs")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCompensationClaimIsExclusiveAndReleasable() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
        return clean()
                .chain(() -> persist("999900001111", "888800001111", 100000L, Status.PENDING, null))
                .chain(id -> transferCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id))
                .chain(id -> transferCommandRepo.claimCompensation(id, "worker-a", "token-a", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                .chain(id -> transferCommandRepo.claimCompensation(id, "worker-b", "token-b", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isFalse())
                        .replaceWith(id))
                .chain(id -> transferCommandRepo.releaseCompensation(id, "worker-a", "token-a",
                        Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)), "retryable dependency failure"))
                .invoke(released -> assertThat(released).isTrue())
                .replaceWithVoid();
    }
}