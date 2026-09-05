package com.sanedge.transaction.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.sanedge.common.enums.Status;
import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.transaction.entity.Transaction;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class TransactionRepositoryTest {

    @Inject
    TransactionQueryRepository transactionQueryRepo;

    @Inject
    TransactionCommandRepository transactionCommandRepo;

    private Uni<Long> persist(String cardNumber, int amount, String paymentMethod,
            int merchantId, Status status, LocalDateTime transactionTime) {
        Transaction tx = new Transaction();
        tx.transactionNo = java.util.UUID.randomUUID();
        tx.cardNumber = cardNumber;
        tx.amount = amount;
        tx.paymentMethod = paymentMethod;
        tx.merchantId = merchantId;
        tx.status = status;
        if (transactionTime != null) {
            tx.transactionTime = Timestamp.valueOf(transactionTime);
        }
        return transactionQueryRepo.persist(tx).map(t -> t.transactionId);
    }

    private Uni<Long> persist(String cardNumber, int amount, String paymentMethod, int merchantId) {
        return persist(cardNumber, amount, paymentMethod, merchantId, Status.PENDING, null);
    }

    private Uni<Long> persist(String cardNumber, int amount) {
        return persist(cardNumber, amount, "CREDIT_CARD", 1);
    }

    private Uni<Void> clean() {
        return transactionQueryRepo.deleteAll().replaceWithVoid();
    }

    private FindAllTransactions findAllReq(int page, int size, String search) {
        FindAllTransactions r = new FindAllTransactions();
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    private FindAllTransactionCardNumber findCardNumberReq(String cardNumber, int page, int size, String search) {
        FindAllTransactionCardNumber r = new FindAllTransactionCardNumber();
        r.setCardNumber(cardNumber);
        r.setPage(page);
        r.setPageSize(size);
        r.setSearch(search == null ? "" : search);
        return r;
    }

    // ==================== Create & Find By ID ====================

    @Test
    @WithTransaction
    Uni<Void> testCreateAndFindById() {
        return clean()
                .chain(() -> persist("4111111111111111", 150000))
                .chain(id -> transactionQueryRepo.findTransactionById(id))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.cardNumber).isEqualTo("4111111111111111");
                    assertThat(found.amount).isEqualTo(150000);
                    assertThat(found.paymentMethod).isEqualTo("CREDIT_CARD");
                    assertThat(found.status).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdReturnsNullWhenNotFound() {
        return clean()
                .chain(() -> transactionQueryRepo.findTransactionById(999999L))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    // ==================== Soft Delete (Trash) Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testTrashTransaction() {
        return clean()
                .chain(() -> persist("4111111111111111", 200000))
                .chain(id -> transactionCommandRepo.trashed(id))
                .invoke(t -> {
                    assertThat(t).isNotNull();
                    assertThat(t.getDeletedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashTransactionReturnsNullIfAlreadyTrashed() {
        return clean()
                .chain(() -> persist("4111111111111122", 200000))
                .chain(id -> transactionCommandRepo.trashed(id)
                        .chain(() -> transactionCommandRepo.trashed(id)))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testTrashTransactionReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transactionCommandRepo.trashed(99999L))
                .invoke(t -> assertThat(t).isNull())
                .replaceWithVoid();
    }

    // ==================== Restore Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransaction() {
        return clean()
                .chain(() -> persist("4222222222222222", 300000))
                .chain(id -> transactionCommandRepo.trashed(id)
                        .chain(() -> transactionCommandRepo.restore(id)))
                .invoke(r -> {
                    assertThat(r).isNotNull();
                    assertThat(r.getDeletedAt()).isNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransactionReturnsNullIfNotTrashed() {
        return clean()
                .chain(() -> persist("4222222222222233", 300000))
                .chain(id -> transactionCommandRepo.restore(id))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreTransactionReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transactionCommandRepo.restore(99999L))
                .invoke(r -> assertThat(r).isNull())
                .replaceWithVoid();
    }

    // ==================== Delete Permanent Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanent() {
        return clean()
                .chain(() -> persist("4333333333333333", 400000))
                .chain(id -> transactionCommandRepo.trashed(id)
                        .chain(() -> transactionCommandRepo.deletePermanent(id))
                        .chain(deleted -> transactionQueryRepo.findTransactionById(id)))
                .invoke(n -> assertThat(n).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentFailsIfNotTrashed() {
        return clean()
                .chain(() -> persist("4333333333333344", 400000))
                .chain(id -> transactionCommandRepo.deletePermanent(id))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeletePermanentReturnsFalseIfNotFound() {
        return clean()
                .chain(() -> transactionCommandRepo.deletePermanent(99999L))
                .invoke(d -> assertThat(d).isFalse())
                .replaceWithVoid();
    }

    // ==================== Bulk Operations Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeleted() {
        return clean()
                .chain(() -> persist("4444444444444444", 500000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("4444444444444455", 600000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("4444444444444466", 700000))
                .chain(() -> transactionCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> transactionQueryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeleted() {
        return clean()
                .chain(() -> persist("5555555555555555", 800000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("5555555555555566", 900000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> persist("5555555555555577", 1000000))
                .chain(() -> transactionCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isTrue())
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testRestoreAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("5555555555555588", 100000))
                .chain(() -> transactionCommandRepo.restoreAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testDeleteAllDeletedReturnsFalseWhenNoTrashed() {
        return clean()
                .chain(() -> persist("5555555555555599", 100000))
                .chain(() -> transactionCommandRepo.deleteAllDeleted())
                .invoke(r -> assertThat(r).isFalse())
                .replaceWithVoid();
    }

    // ==================== Update Status Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus() {
        return clean()
                .chain(() -> persist("6666666666666666", 100000, "CREDIT_CARD", 1, Status.PENDING, null))
                .chain(id -> transactionCommandRepo.updateTransactionStatus(id, "SUCCESS"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.status).isEqualTo(Status.SUCCESS);
                    assertThat(updated.getUpdatedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusToFailed() {
        return clean()
                .chain(() -> persist("6666666666666677", 100000, "CREDIT_CARD", 1, Status.PENDING, null))
                .chain(id -> transactionCommandRepo.updateTransactionStatus(id, "FAILED"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.status).isEqualTo(Status.FAILED);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusReturnsNullIfNotFound() {
        return clean()
                .chain(() -> transactionCommandRepo.updateTransactionStatus(99999L, "SUCCESS"))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusInvalidStatusDoesNotChange() {
        return clean()
                .chain(() -> persist("6666666666666688", 100000, "CREDIT_CARD", 1, Status.PENDING, null))
                .chain(id -> transactionCommandRepo.updateTransactionStatus(id, "INVALID_STATUS"))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.status).isEqualTo(Status.PENDING);
                })
                .replaceWithVoid();
    }

    // ==================== Query - Active/Trashed Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindActiveTransactions() {
        return clean()
                .chain(() -> persist("7777777777777777", 100000))
                .chain(() -> persist("7777777777777788", 200000))
                .chain(() -> transactionQueryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedTransactions() {
        return clean()
                .chain(() -> persist("8888888888888888", 100000))
                .chain(() -> persist("8888888888888899", 200000))
                .chain(() -> transactionQueryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindActiveTransactionsExcludesTrashed() {
        return clean()
                .chain(() -> persist("8888888888888800", 100000))
                .chain(() -> persist("8888888888888811", 200000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transactionQueryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTrashedTransactionsOnlyShowsTrashed() {
        return clean()
                .chain(() -> persist("8888888888888822", 100000))
                .chain(() -> persist("8888888888888833", 200000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transactionQueryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).cardNumber).isEqualTo("8888888888888833");
                })
                .replaceWithVoid();
    }

    // ==================== Query - Search Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithSearchByCardNumber() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000))
                .chain(() -> persist("9876543210987654", 200000))
                .chain(() -> persist("1111222233334444", 300000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "1234")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).cardNumber).isEqualTo("1234567890123456");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithSearchByPaymentMethod() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000, "E_WALLET", 1))
                .chain(() -> persist("9876543210987654", 200000, "CREDIT_CARD", 1))
                .chain(() -> persist("1111222233334444", 300000, "DEBIT_CARD", 1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "E_WALLET")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).paymentMethod).isEqualTo("E_WALLET");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithSearchByStatus() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000, "CREDIT_CARD", 1, Status.SUCCESS, null))
                .chain(() -> persist("9876543210987654", 200000, "CREDIT_CARD", 1, Status.FAILED, null))
                .chain(() -> persist("1111222233334444", 300000, "CREDIT_CARD", 1, Status.PENDING, null))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "success")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).status).isEqualTo(Status.SUCCESS);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithEmptySearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000))
                .chain(() -> persist("9876543210987654", 200000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithNullSearchReturnsAll() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000))
                .chain(() -> persist("9876543210987654", 200000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, null)))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    // ==================== Query - Pagination Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsWithPagination() {
        return clean()
                .chain(() -> persist("11110001", 100000))
                .chain(() -> persist("11110002", 200000))
                .chain(() -> persist("11110003", 300000))
                .chain(() -> persist("11110004", 400000))
                .chain(() -> persist("11110005", 500000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 2, "")))
                .map(page1 -> {
                    assertThat(page1.getData()).hasSize(2);
                    assertThat(page1.getTotalRecords()).isEqualTo(5);
                    return page1;
                })
                .chain(page1 -> transactionQueryRepo.findTransactions(findAllReq(2, 2, "")))
                .invoke(page2 -> assertThat(page2.getData()).hasSize(2))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(3, 2, "")))
                .invoke(page3 -> assertThat(page3.getData()).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsPageZeroDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("11110006", 100000))
                .chain(() -> persist("11110007", 200000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(0, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsNegativePageDefaultsToFirstPage() {
        return clean()
                .chain(() -> persist("11110008", 100000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(-1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsZeroSizeDefaultsToTen() {
        return clean()
                .chain(() -> persist("11110009", 100000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 0, "")))
                .invoke(r -> assertThat(r.getData()).hasSizeLessThanOrEqualTo(10))
                .replaceWithVoid();
    }

    // ==================== Query - Sort Order Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsSortedByTransactionTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 3, 10, 0, 0);

        return clean()
                .chain(() -> persist("11110010", 100000, "CREDIT_CARD", 1, Status.PENDING, time1))
                .chain(() -> persist("11110011", 200000, "CREDIT_CARD", 1, Status.PENDING, time2))
                .chain(() -> persist("11110012", 300000, "CREDIT_CARD", 1, Status.PENDING, time3))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(3);
                    assertThat(r.getData().get(0).transactionTime.toLocalDateTime()).isEqualTo(time3);
                    assertThat(r.getData().get(1).transactionTime.toLocalDateTime()).isEqualTo(time2);
                    assertThat(r.getData().get(2).transactionTime.toLocalDateTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    // ==================== Query - By Card Number Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByCardNumber() {
        return clean()
                .chain(() -> persist("1234123412341234", 100000))
                .chain(() -> persist("5678567856785678", 200000))
                .chain(() -> persist("1234123412341234", 300000))
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341234", 1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(2);
                    assertThat(r.getData().stream().allMatch(t -> t.cardNumber.equals("1234123412341234")))
                            .isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByCardNumberExcludesTrashed() {
        return clean()
                .chain(() -> persist("1234123412341234", 100000))
                .chain(() -> persist("1234123412341234", 200000)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341234", 1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByCardNumberSortedByTransactionTimeDescending() {
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 10, 0, 0);

        return clean()
                .chain(() -> persist("1234123412341235", 100000, "CREDIT_CARD", 1, Status.PENDING, time1))
                .chain(() -> persist("1234123412341235", 200000, "CREDIT_CARD", 1, Status.PENDING, time2))
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341235", 1, 10, "")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(2);
                    assertThat(r.getData().get(0).transactionTime.toLocalDateTime()).isEqualTo(time2);
                    assertThat(r.getData().get(1).transactionTime.toLocalDateTime()).isEqualTo(time1);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByCardNumberWithSearchByPaymentMethod() {
        return clean()
                .chain(() -> persist("1234123412341236", 100000, "CREDIT_CARD", 1))
                .chain(() -> persist("1234123412341236", 200000, "E_WALLET", 1))
                .chain(() -> persist("1234123412341236", 300000, "DEBIT_CARD", 1))
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341236", 1, 10, "E_WALLET")))
                .invoke(r -> {
                    assertThat(r.getTotalRecords()).isEqualTo(1);
                    assertThat(r.getData().get(0).paymentMethod).isEqualTo("E_WALLET");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByCardNumberWithPagination() {
        return clean()
                .chain(() -> persist("1234123412341237", 100000))
                .chain(() -> persist("1234123412341237", 200000))
                .chain(() -> persist("1234123412341237", 300000))
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341237", 1, 2, "")))
                .invoke(r -> {
                    assertThat(r.getData()).hasSize(2);
                    assertThat(r.getTotalRecords()).isEqualTo(3);
                })
                .chain(() -> transactionQueryRepo.findTransactionsByCardNumber(
                        findCardNumberReq("1234123412341237", 2, 2, "")))
                .invoke(r -> assertThat(r.getData()).hasSize(1))
                .replaceWithVoid();
    }

    // ==================== Query - By Merchant ID Tests ====================

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByMerchantId() {
        return clean()
                .chain(() -> persist("4111111111111111", 100000, "CREDIT_CARD", 10))
                .chain(() -> persist("4222222222222222", 200000, "CREDIT_CARD", 10))
                .chain(() -> persist("4333333333333333", 300000, "CREDIT_CARD", 20))
                .chain(() -> transactionQueryRepo.findTransactionsByMerchantId(10L))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().allMatch(t -> t.merchantId == 10)).isTrue();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByMerchantIdExcludesTrashed() {
        return clean()
                .chain(() -> persist("4111111111111111", 100000, "CREDIT_CARD", 10))
                .chain(() -> persist("4222222222222222", 200000, "CREDIT_CARD", 10)
                        .chain(id -> transactionCommandRepo.trashed(id).replaceWithVoid()))
                .chain(() -> transactionQueryRepo.findTransactionsByMerchantId(10L))
                .invoke(list -> assertThat(list).hasSize(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindTransactionsByMerchantIdReturnsEmptyWhenNoMatch() {
        return clean()
                .chain(() -> persist("4111111111111111", 100000, "CREDIT_CARD", 10))
                .chain(() -> transactionQueryRepo.findTransactionsByMerchantId(99L))
                .invoke(list -> assertThat(list).isEmpty())
                .replaceWithVoid();
    }

    // ==================== Edge Cases ====================

    @Test
    @WithTransaction
    Uni<Void> testEmptyDatabaseReturnsZeroRecords() {
        return clean()
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> transactionQueryRepo.findActiveTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .chain(() -> transactionQueryRepo.findTrashedTransactions(findAllReq(1, 10, "")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSearchWithNoMatchesReturnsZeroRecords() {
        return clean()
                .chain(() -> persist("1234567890123456", 100000))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "NONEXISTENT_SEARCH")))
                .invoke(r -> assertThat(r.getTotalRecords()).isZero())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatusOnTrashedTransactionReturnsNull() {
        return clean()
                .chain(() -> persist("9999888877776666", 100000))
                .chain(id -> transactionCommandRepo.trashed(id)
                        .chain(() -> transactionCommandRepo.updateTransactionStatus(id, "SUCCESS")))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCaseInsensitiveStatusSearch() {
        return clean()
                .chain(() -> persist("1111222233445566", 100000, "CREDIT_CARD", 1, Status.SUCCESS, null))
                .chain(() -> persist("1111222233445577", 200000, "CREDIT_CARD", 1, Status.FAILED, null))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "SUCCESS")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "success")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "SuCceSs")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCaseInsensitivePaymentMethodSearch() {
        return clean()
                .chain(() -> persist("1111222233445588", 100000, "CREDIT_CARD", 1))
                .chain(() -> persist("1111222233445599", 200000, "E_WALLET", 1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "credit_card")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "CREDIT_CARD")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .chain(() -> transactionQueryRepo.findTransactions(findAllReq(1, 10, "CrEdIt_CaRd")))
                .invoke(r -> assertThat(r.getTotalRecords()).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCompensationClaimIsExclusiveAndReleasable() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseUntil = Timestamp.valueOf(LocalDateTime.now().plusMinutes(5));
        return clean()
                .chain(() -> persist("9999000011112222", 100000, "CREDIT_CARD", 1, Status.PENDING, null))
                .chain(id -> transactionCommandRepo.markCompensationRequired(id, "saldo update failed").replaceWith(id))
                .chain(id -> transactionCommandRepo.claimCompensation(id, "worker-a", "token-a", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isTrue())
                        .replaceWith(id))
                .chain(id -> transactionCommandRepo.claimCompensation(id, "worker-b", "token-b", now, leaseUntil, 3)
                        .invoke(claimed -> assertThat(claimed).isFalse())
                        .replaceWith(id))
                .chain(id -> transactionCommandRepo.releaseCompensation(id, "worker-a", "token-a",
                        Timestamp.valueOf(LocalDateTime.now().minusSeconds(1)), "retryable dependency failure"))
                .invoke(released -> assertThat(released).isTrue())
                .replaceWithVoid();
    }
}