package com.sanedge.withdraw.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.entity.Withdraw;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import com.sanedge.common.test.PostgreSqlResource;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(PostgreSqlResource.class)
@QuarkusTest
class WithdrawRepositoryTest {

    @Inject
    WithdrawQueryRepository withdrawQueryRepo;

    @Inject
    WithdrawCommandRepository withdrawCommandRepo;

    private Uni<Long> persist(String cardNumber, Long amount, Status status, LocalDateTime withdrawTime) {
        Withdraw withdraw = new Withdraw();
        withdraw.setCardNumber(cardNumber);
        withdraw.setWithdrawAmount(amount.intValue());
        withdraw.setStatus(status);
        if (withdrawTime != null) {
            withdraw.setWithdrawTime(Timestamp.valueOf(withdrawTime));
        } else {
            withdraw.setWithdrawTime(Timestamp.valueOf(LocalDateTime.now()));
        }
        withdraw.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        withdraw.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return withdrawQueryRepo.persist(withdraw).map(w -> w.getWithdrawId());
    }

    private Uni<Long> persist(String cardNumber, Long amount) {
        return persist(cardNumber, amount, Status.PENDING, null);
    }

    private Uni<Void> clean() {
        return withdrawQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllWithdraws findAllReq(int page, int size, String search) {
        FindAllWithdraws req = new FindAllWithdraws();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search);
        return req;
    }

    private FindAllWithdrawCardNumber findAllByCardNumberReq(String cardNumber, int page, int size, String search) {
        FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
        req.setCardNumber(cardNumber);
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search);
        return req;
    }

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("1234567890", 150000L))
                .chain(id -> withdrawQueryRepo.findById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getCardNumber()).isEqualTo("1234567890");
                    assertThat(found.getWithdrawAmount()).isEqualTo(150000L);
                    assertThat(found.getStatus()).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> withdrawQueryRepo.findById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashWithdraw() {
        return clean()
                .chain(() -> persist("111122223333", 200000L))
                .chain(id -> withdrawCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashWithdrawReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("111122223344", 200000L))
                .chain(id -> withdrawCommandRepo.trashed(id)
                        .chain(() -> withdrawCommandRepo.trashed(id)))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashWithdrawReturnsNullIfNotFound() {
        return clean()
                .chain(() -> withdrawCommandRepo.trashed(99999L))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    // ==================== Restore Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreWithdraw() {
        return clean()
                .chain(() -> persist("222233334444", 300000L))
                .chain(id -> withdrawCommandRepo.trashed(id)
                        .chain(() -> withdrawCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreWithdrawReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("222233334455", 300000L))
                .chain(id -> withdrawCommandRepo.restore(id))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreWithdrawReturnsNullIfNotFound() {
        return clean()
                .chain(() -> withdrawCommandRepo.restore(99999L))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    // ==================== Delete Permanent Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("333344445555", 400000L))
                .chain(id -> withdrawCommandRepo.trashed(id)
                        .chain(() -> withdrawCommandRepo.deletePermanent(id))
                        .chain(deleted -> withdrawQueryRepo.findById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persist("333344445566", 400000L))
                .chain(id -> withdrawCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentReturnsFalseIfNotFound() {
        return clean()
                .chain(() -> withdrawCommandRepo.deletePermanent(99999L))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("444455556666", 500000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556677", 600000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("444455556688", 700000L))
                .chain(() -> withdrawCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> withdrawQueryRepo.findTrashedWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("555566667777", 800000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667788", 900000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("555566667799", 1000000L))
                .chain(() -> withdrawCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667800", 100000L))
                .chain(() -> withdrawCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("555566667811", 100000L))
                .chain(() -> withdrawCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    // ==================== Update Status Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus() {
        return clean()
                .chain(() -> persist("666677778888", 100000L, Status.PENDING, null))
                .chain(id -> withdrawCommandRepo.updateStatus(id, "SUCCESS"))
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
                .chain(() -> persist("666677778899", 100000L, Status.PENDING, null))
                .chain(id -> withdrawCommandRepo.updateStatus(id, "FAILED"))
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
                .chain(() -> withdrawCommandRepo.updateStatus(99999L, "SUCCESS"))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusInvalidStatusDoesNotChange() {
        return clean()
                .chain(() -> persist("666677778900", 100000L, Status.PENDING, null))
                .chain(id -> withdrawCommandRepo.updateStatus(id, "INVALID_STATUS"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getStatus()).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    // ==================== Query - Active/Trashed Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveWithdraws() {
        return clean()
                .chain(() -> persist("777788889999", 100000L))
                .chain(() -> persist("777788889900", 200000L))
                .chain(() -> withdrawQueryRepo.findActiveWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedWithdraws() {
        return clean()
                .chain(() -> persist("888899990000", 100000L))
                .chain(() -> persist("888899990011", 200000L))
                .chain(() -> withdrawQueryRepo.findTrashedWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveWithdrawsExcludesTrashed() {
        return clean()
                .chain(() -> persist("888899990022", 100000L))
                .chain(() -> persist("888899990033", 200000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> withdrawQueryRepo.findActiveWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedWithdrawsOnlyShowsTrashed() {
        return clean()
                .chain(() -> persist("888899990044", 100000L))
                .chain(() -> persist("888899990055", 200000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> withdrawQueryRepo.findTrashedWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("888899990055");
                })
                .replaceWithVoid();
    }

    // ==================== Query - Search Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithSearchByCardNumber() {
        return clean()
                .chain(() -> persist("1234567890123", 100000L))
                .chain(() -> persist("9876543210987", 200000L))
                .chain(() -> persist("1111222233333", 300000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "1234")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getCardNumber()).isEqualTo("1234567890123");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithSearchByAmount() {
        return clean()
                .chain(() -> persist("1234567890123", 150000L))
                .chain(() -> persist("9876543210987", 250000L))
                .chain(() -> persist("1111222233333", 350000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "150000")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getWithdrawAmount()).isEqualTo(150000L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithSearchByStatus() {
        return clean()
                .chain(() -> persist("1234567890123", 100000L, Status.SUCCESS, null))
                .chain(() -> persist("9876543210987", 200000L, Status.FAILED, null))
                .chain(() -> persist("1111222233333", 300000L, Status.PENDING, null))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "success")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getStatus()).isEqualTo(Status.SUCCESS);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithEmptySearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123", 100000L))
                .chain(() -> persist("9876543210987", 200000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithNullSearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123", 100000L))
                .chain(() -> persist("9876543210987", 200000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, null)))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    // ==================== Query - Pagination Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsWithPagination() {
        return clean()
                .chain(() -> persist("1111111111001", 100000L))
                .chain(() -> persist("1111111111002", 200000L))
                .chain(() -> persist("1111111111003", 300000L))
                .chain(() -> persist("1111111111004", 400000L))
                .chain(() -> persist("1111111111005", 500000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 2, "")))
                .map(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                    return page1;
                })
                .chain(page1 -> withdrawQueryRepo.findAllWithdraws(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsPageZeroDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("1111111111006", 100000L))
                .chain(() -> persist("1111111111007", 200000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(0, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsNegativePageDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("1111111111008", 100000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(-1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsZeroSizeDefaultsToTen() {
        return clean()
                .chain(() -> persist("1111111111009", 100000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 0, "")))
                .invoke(r -> assertThat(r.getData()).hasSizeLessThanOrEqualTo(10))
                .replaceWithVoid();
    }

    // ==================== Query - Sort Order Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindWithdrawsSortedByWithdrawTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 3, 10, 0, 0);

        return clean()
                .chain(() -> persist("1111111111010", 100000L, Status.PENDING, time1))
                .chain(() -> persist("1111111111011", 200000L, Status.PENDING, time2))
                .chain(() -> persist("1111111111012", 300000L, Status.PENDING, time3))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(3);
                    assertThat(r.getData().get(0).getWithdrawTime()).isEqualTo(time3);
                    assertThat(r.getData().get(1).getWithdrawTime()).isEqualTo(time2);
                    assertThat(r.getData().get(2).getWithdrawTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    // ==================== Query - By Card Number Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumber() {
        return clean()
                .chain(() -> persist("123412341234", 100000L))
                .chain(() -> persist("123412341234", 200000L))
                .chain(() -> persist("999999999999", 300000L))
                .chain(() -> withdrawQueryRepo.findByCardNumber("123412341234"))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().allMatch(w -> w.getCardNumber().equals("123412341234"))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumberReturnsEmptyWhenNotFound() {
        return clean()
                .chain(() -> persist("123412341234", 100000L))
                .chain(() -> withdrawQueryRepo.findByCardNumber("999999999999"))
                .invoke(list -> assertThat(list).isEmpty())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumberExcludesTrashed() {
        return clean()
                .chain(() -> persist("123412341234", 100000L))
                .chain(() -> persist("123412341234", 200000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> withdrawQueryRepo.findByCardNumber("123412341234"))
                .invoke(list -> assertThat(list).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumberSortedByWithdrawTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);

        return clean()
                .chain(() -> persist("123412341235", 100000L, Status.PENDING, time1))
                .chain(() -> persist("123412341235", 200000L, Status.PENDING, time2))
                .chain(() -> withdrawQueryRepo.findByCardNumber("123412341235"))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.get(0).getWithdrawTime()).isEqualTo(time2);
                    assertThat(list.get(1).getWithdrawTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    // ==================== Query - Paged By Card Number Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumber() {
        return clean()
                .chain(() -> persist("567856785678", 100000L))
                .chain(() -> persist("567856785678", 200000L))
                .chain(() -> persist("999999999999", 300000L))
                .chain(() -> withdrawQueryRepo.findAllByCardNumber(findAllByCardNumberReq("567856785678", 1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(2);
                    assertThat(r.getData().stream().allMatch(w -> w.getCardNumber().equals("567856785678"))).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumberWithSearchByAmount() {
        return clean()
                .chain(() -> persist("567856785679", 150000L))
                .chain(() -> persist("567856785679", 250000L))
                .chain(() -> persist("567856785679", 350000L))
                .chain(() -> withdrawQueryRepo
                        .findAllByCardNumber(findAllByCardNumberReq("567856785679", 1, 10, "150000")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getWithdrawAmount()).isEqualTo(150000L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumberWithSearchByStatus() {
        return clean()
                .chain(() -> persist("567856785680", 100000L, Status.SUCCESS, null))
                .chain(() -> persist("567856785680", 200000L, Status.FAILED, null))
                .chain(() -> withdrawQueryRepo
                        .findAllByCardNumber(findAllByCardNumberReq("567856785680", 1, 10, "failed")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).getStatus()).isEqualTo(Status.FAILED);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumberWithPagination() {
        return clean()
                .chain(() -> persist("567856785681", 100000L))
                .chain(() -> persist("567856785681", 200000L))
                .chain(() -> persist("567856785681", 300000L))
                .chain(() -> persist("567856785681", 400000L))
                .chain(() -> withdrawQueryRepo.findAllByCardNumber(findAllByCardNumberReq("567856785681", 1, 2, "")))
                .map(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(4);
                    return page1;
                })
                .chain(page1 -> withdrawQueryRepo.findAllByCardNumber(findAllByCardNumberReq("567856785681", 2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumberExcludesTrashed() {
        return clean()
                .chain(() -> persist("567856785682", 100000L))
                .chain(() -> persist("567856785682", 200000L)
                        .chain(id -> withdrawCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> withdrawQueryRepo.findAllByCardNumber(findAllByCardNumberReq("567856785682", 1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindAllByCardNumberReturnsEmptyForInvalidCardNumber() {
        return clean()
                .chain(() -> persist("567856785683", 100000L))
                .chain(() -> withdrawQueryRepo.findAllByCardNumber(findAllByCardNumberReq("000000000000", 1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> withdrawQueryRepo.findActiveWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> withdrawQueryRepo.findTrashedWithdraws(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchWithNoMatchesReturnsZeroRecords() {
        return clean()
                .chain(() -> persist("1234567890123", 100000L))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "NONEXISTENT_SEARCH")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusOnTrashedWithdrawReturnsNull() {
        return clean()
                .chain(() -> persist("999988887777", 100000L))
                .chain(id -> withdrawCommandRepo.trashed(id)
                        .chain(() -> withdrawCommandRepo.updateStatus(id, "SUCCESS")))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCaseInsensitiveStatusSearch() {
        return clean()
                .chain(() -> persist("111122223344", 100000L, Status.SUCCESS, null))
                .chain(() -> persist("111122223355", 200000L, Status.FAILED, null))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "SUCCESS")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "success")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> withdrawQueryRepo.findAllWithdraws(findAllReq(1, 10, "SuCceSs")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCompensationClaimIsExclusiveAndReleasable() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
        return clean()
                .chain(() -> persist("999900001111", 100000L, Status.PENDING, null))
                .chain(id -> withdrawCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id))
                .chain(id -> withdrawCommandRepo.claimCompensation(id, "worker-a", "token-a", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                .chain(id -> withdrawCommandRepo.claimCompensation(id, "worker-b", "token-b", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isFalse())
                        .replaceWith(id))
                .chain(id -> withdrawCommandRepo.releaseCompensation(id, "worker-a", "token-a",
                        Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)), "retryable dependency failure"))
                .invoke(released -> assertThat(released).isTrue())
                .replaceWithVoid();
    }
}