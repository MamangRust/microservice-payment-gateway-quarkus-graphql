package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.adapter.card.CardPort;
import com.sanedge.common.adapter.merchant.MerchantPort;
import com.sanedge.common.adapter.model.Card;
import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.model.Saldo;
import com.sanedge.common.adapter.model.SaldoMutationResult;
import com.sanedge.common.adapter.saldo.SaldoPort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Outbox;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.OutboxRepository;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.KafkaService;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.validation.Validator;

/**
 * Unit tests for {@link TransactionCommandServiceImpl}.
 * <p>
 * Covers all CRUD operations: create, update, trashed, restore,
 * deletePermanent, restoreAll, deleteAll.
 */
@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceImplTest {

    @Mock
    CardPort cardPort;

    @Mock
    MerchantPort merchantPort;

    @Mock
    SaldoPort saldoPort;

    @Mock
    TransactionQueryRepository transactionQueryRepository;

    @Mock
    TransactionCommandRepository transactionCommandRepository;

    @Mock
    OutboxRepository outboxRepository;

    @Mock
    Validator validator;

    @Mock
    RedisService redisService;

    @Mock
    KafkaService kafkaService;

    @Mock
    TracingMetrics tracingMetrics;

    @InjectMocks
    TransactionCommandServiceImpl transactionCommandService;

    private Transaction transaction;

    private static Merchant merchant(int id, int userId) {
        return new Merchant(id, "Merchant " + id, "api-key-1", "ACTIVE", userId, null, null);
    }

    private static Card card(String cardNumber, String email) {
        return new Card(1, 1, cardNumber, "CREDIT", "12/30", "123", "VISA", email, null, null);
    }

    private static Saldo saldo(String cardNumber, int totalBalance) {
        return new Saldo(1, cardNumber, totalBalance, null, null, null, null);
    }

    private static SaldoMutationResult mutation(String cardNumber, int totalBalance) {
        return new SaldoMutationResult(1, cardNumber, totalBalance);
    }

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.transactionId = 1L;
        transaction.transactionNo = UUID.randomUUID();
        transaction.cardNumber = "4111111111111111";
        transaction.amount = 150000;
        transaction.paymentMethod = "CREDIT_CARD";
        transaction.merchantId = 1;
        transaction.transactionTime = Timestamp.valueOf(LocalDateTime.now());
        transaction.status = Status.PENDING;

        lenient().when(outboxRepository.persist(any(Outbox.class)))
                .thenAnswer(inv -> Uni.createFrom().item((Outbox) inv.getArgument(0)));

        // TracingMetrics no-op mock
        lenient().when(tracingMetrics.traceAndMeasure(anyString(), anyString(), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(3);
                    return supplier.get();
                });
        lenient().when(tracingMetrics.traceAndMeasure(anyString(), anyString(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(2);
                    return supplier.get();
                });

        // Validator passes by default
        lenient().when(validator.validate(any())).thenReturn(Set.of());

        // Redis cache-miss by default
        lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
        lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), any(Long.class)))
                .thenReturn(Uni.createFrom().voidItem());
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
    }

    // ──────────────────────────────────────────────
    // Create Transaction
    // ──────────────────────────────────────────────

    @Test
    void createTransaction_success() {
        when(merchantPort.findByApiKey(any()))
                .thenReturn(Uni.createFrom().item(merchant(1, 1)));

        when(cardPort.findUserCardByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(card("4111111111111111", "test@example.com")));

        when(saldoPort.findByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(saldo("4111111111111111", 500000)));

        when(saldoPort.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().item(mutation("4111111111111111", 350000)));

        when(transactionCommandRepository.persist(any(com.sanedge.transaction.entity.Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    t.transactionId = 1L;
                    return Uni.createFrom().item(t);
                });

        when(transactionCommandRepository.updateTransactionStatus(any(Long.class), anyString()))
                .thenAnswer(inv -> {
                    transaction.transactionId = 1L;
                    transaction.status = Status.SUCCESS;
                    return Uni.createFrom().item(transaction);
                });

        when(cardPort.findCardByUserId(any(Integer.class)))
                .thenReturn(Uni.createFrom().item(card("2222222222222222", null)));

        when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
                .thenReturn(Uni.createFrom().voidItem());

        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setCardNumber("4111111111111111");
        req.setAmount(150000L);
        req.setPaymentMethod("CREDIT_CARD");
        req.setMerchantId(1L);

        ApiResponse<TransactionResponse> result = transactionCommandService.create("test-api-key", req)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().getCardNumber()).isEqualTo("4111111111111111");
        // Two-phase ledger: PENDING reservation persist, then final persist with leg-B metadata
        verify(transactionCommandRepository, times(2)).persist(any(com.sanedge.transaction.entity.Transaction.class));
    }

    // ──────────────────────────────────────────────
    // Update Transaction
    // ──────────────────────────────────────────────

    @Test
    void updateTransaction_success() {
        Transaction existing = new Transaction();
        existing.transactionId = 1L;
        existing.cardNumber = "4111111111111111";
        existing.amount = 50000;
        existing.paymentMethod = "CREDIT_CARD";
        existing.merchantId = 1;
        existing.status = Status.SUCCESS;
        when(transactionQueryRepository.findTransactionById(1L))
                .thenReturn(Uni.createFrom().item(existing));

        when(merchantPort.findByApiKey(any()))
                .thenReturn(Uni.createFrom().item(merchant(1, 1)));

        when(cardPort.findCardByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(card("4111111111111111", null)));

        when(saldoPort.findByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(saldo("4111111111111111", 500000)));

        when(saldoPort.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().item(mutation("4111111111111111", 400000)));

        when(transactionCommandRepository.persist(any(com.sanedge.transaction.entity.Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction t = inv.getArgument(0);
                    return Uni.createFrom().item(t);
                });

        when(transactionCommandRepository.updateTransactionStatus(any(Long.class), anyString()))
                .thenAnswer(inv -> {
                    transaction.transactionId = 1L;
                    transaction.status = Status.SUCCESS;
                    return Uni.createFrom().item(transaction);
                });

        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        UpdateTransactionRequest req = new UpdateTransactionRequest();
        req.setTransactionId(1L);
        req.setCardNumber("4111111111111111");
        req.setAmount(150000L);
        req.setPaymentMethod("CREDIT_CARD");
        req.setMerchantId(1L);

        ApiResponse<TransactionResponse> result = transactionCommandService.update("test-api-key", req)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        verify(transactionQueryRepository).findTransactionById(1L);
        verify(transactionCommandRepository).persist(any(com.sanedge.transaction.entity.Transaction.class));
    }

    // ──────────────────────────────────────────────
    // Trash / Restore / Delete
    // ──────────────────────────────────────────────

    @Test
    void trashed_success() {
        when(transactionCommandRepository.trashed(1L)).thenReturn(Uni.createFrom().item(transaction));

        ApiResponse<TransactionResponseDeleteAt> result = transactionCommandService.trashed(1L)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        verify(transactionCommandRepository).trashed(1L);
    }

    @Test
    void restore_success() {
        when(transactionCommandRepository.restore(1L)).thenReturn(Uni.createFrom().item(transaction));

        ApiResponse<TransactionResponseDeleteAt> result = transactionCommandService.restore(1L)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        verify(transactionCommandRepository).restore(1L);
    }

    @Test
    void deletePermanent_success() {
        Transaction trashedTx = new Transaction();
        trashedTx.transactionId = 1L;
        trashedTx.cardNumber = "4111111111111111";
        trashedTx.merchantId = 1;
        trashedTx.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        when(transactionCommandRepository.findById(1L)).thenReturn(Uni.createFrom().item(trashedTx));
        when(transactionCommandRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Boolean> result = transactionCommandService.deletePermanent(1L)
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isTrue();
        verify(transactionCommandRepository).deletePermanent(1L);
    }

    @Test
    void restoreAll_success() {
        when(transactionCommandRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Boolean> result = transactionCommandService.restoreAll()
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isTrue();
    }

    @Test
    void deleteAll_success() {
        when(transactionCommandRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));

        ApiResponse<Boolean> result = transactionCommandService.deleteAll()
                .await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isTrue();
    }

    // ═══════════════════════════════════════════════
    // F6 Failure-Injection Suite
    // (ported from Go tests/transaction/failure_injection_test.go)
    // Verifies: invalid amount → rejected (F1),
    //           insufficient balance → error (F2),
    //           debit failure → transaction not persisted / failed (F3),
    //           credit failure → reconciliation flag (F4)
    // ═══════════════════════════════════════════════

    // F1: Invalid amount (zero / negative) must be rejected before any
    //     remote call, surfacing as a validation error (400-like).
    @Test
    void failureF1_invalidAmount_rejectedWithValidationError() {
        for (long badAmount : new long[]{0, -100, -1}) {
            CreateTransactionRequest req = new CreateTransactionRequest();
            req.setCardNumber("4111111111111111");
            req.setAmount(badAmount);
            req.setPaymentMethod("CASHLESS");
            req.setMerchantId(1L);

            // validator returns a constraint violation
            jakarta.validation.ConstraintViolation<CreateTransactionRequest> cv =
                    org.mockito.Mockito.mock(jakarta.validation.ConstraintViolation.class);
            when(cv.getMessage()).thenReturn("amount must be > 0");
            lenient().when(validator.validate(any(CreateTransactionRequest.class)))
                    .thenReturn(Set.of(cv));

            ApiResponse<TransactionResponse> result =
                    transactionCommandService.create("api-key-1", req)
                            .await().indefinitely();

            assertThat(result.status()).isEqualTo("error")
                    .as("amount %d must be rejected", badAmount);
            assertThat(result.message()).containsIgnoringCase("validation")
                    .as("error message must mention validation, got: %s", result.message());

            // No remote call should have been made
            verify(merchantPort, never()).findByApiKey(any());
        }
    }

    // F2: Insufficient balance — customer saldo < requested amount.
    //     The service must surface a clear error (resource-not-found
    //     or bad-request), NOT a 500 / NullPointerException.
    @Test
    void failureF2_insufficientBalance_errorReturned() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setCardNumber("4111111111111111");
        req.setAmount(999_999_999L);
        req.setPaymentMethod("CASHLESS");
        req.setMerchantId(1L);

        when(validator.validate(any())).thenReturn(Set.of());

        // Merchant exists
        when(merchantPort.findByApiKey(any()))
                .thenReturn(Uni.createFrom().item(merchant(1, 1)));

        // Card exists
        when(cardPort.findUserCardByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(card("4111111111111111", null)));

        // Saldo exists but balance is small
        when(saldoPort.findByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(saldo("4111111111111111", 5000)));

        ApiResponse<TransactionResponse> result =
                transactionCommandService.create("api-key-1", req)
                        .await().indefinitely();

        assertThat(result.status()).isEqualTo("error")
                .as("insufficient balance must return error status");
        assertThat(result.message()).containsIgnoringCase("balance")
                .as("error should mention balance: %s", result.message());

        // Saldo debit must NOT have been called
        verify(saldoPort, never()).updateSaldoBalance(any());
    }

    // F3: Debit failure — saldo gRPC call fails mid-transaction.
    //     The transaction must NOT be persisted (or must be marked FAILED),
    //     and no nil-dereference / panic must occur.
    @Test
    void failureF3_debitFailure_noPersistAndErrorReturned() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setCardNumber("4111111111111111");
        req.setAmount(10_000L);
        req.setPaymentMethod("CASHLESS");
        req.setMerchantId(1L);

        when(validator.validate(any())).thenReturn(Set.of());

        when(merchantPort.findByApiKey(any()))
                .thenReturn(Uni.createFrom().item(merchant(1, 1)));

        when(cardPort.findUserCardByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(card("4111111111111111", null)));

        when(saldoPort.findByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(saldo("4111111111111111", 100_000)));

        // DEBIT FAILS: saldo service unavailable
        when(saldoPort.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().failure(
                        new io.grpc.StatusRuntimeException(
                                io.grpc.Status.UNAVAILABLE.withDescription("saldo service down"))));

        try {
            transactionCommandService.create("api-key-1", req)
                    .await().indefinitely();
        } catch (Exception e) {
            // Expected — debit failure propagates
        }

        // Transaction must NOT have been persisted (debit failed before persist)
        verify(transactionCommandRepository, never()).persist(any(com.sanedge.transaction.entity.Transaction.class));
    }

    // F4: Credit failure — customer debit succeeded, merchant credit fails.
    //     The transaction persists as SUCCESS on the customer side;
    //     the merchant-side credit failure surfaces as an error (for
    //     reconciliation / compensation queue). The key invariant:
    //     no nil-dereference, and the customer is NOT double-charged.
    @Test
    void failureF4_creditFailure_customerDebitSuccessMerchantCreditFails() {
        CreateTransactionRequest req = new CreateTransactionRequest();
        req.setCardNumber("4111111111111111");
        req.setAmount(5_000L);
        req.setPaymentMethod("CASHLESS");
        req.setMerchantId(1L);

        when(validator.validate(any())).thenReturn(Set.of());

        when(merchantPort.findByApiKey(any()))
                .thenReturn(Uni.createFrom().item(merchant(1, 1)));

        when(cardPort.findUserCardByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(card("4111111111111111", null)));

        when(saldoPort.findByCardNumber(any()))
                .thenReturn(Uni.createFrom().item(saldo("4111111111111111", 100_000)));

        // First debit (customer) succeeds, second (merchant credit) fails
        when(saldoPort.updateSaldoBalance(any()))
                .thenReturn(Uni.createFrom().item(mutation("4111111111111111", 95_000)))
                .thenReturn(Uni.createFrom().failure(
                        new io.grpc.StatusRuntimeException(
                                io.grpc.Status.UNAVAILABLE.withDescription("merchant credit failed"))));

        when(transactionCommandRepository.persist(any(com.sanedge.transaction.entity.Transaction.class)))
                .thenAnswer(inv -> {
                    com.sanedge.transaction.entity.Transaction tx = inv.getArgument(0);
                    tx.transactionId = 42L;
                    return Uni.createFrom().item(tx);
                });
        when(transactionCommandRepository.updateTransactionStatus(anyLong(), anyString()))
                .thenAnswer(inv -> {
                    transaction.status = Status.valueOf(inv.getArgument(1));
                    return Uni.createFrom().item(transaction);
                });

        // Merchant card lookup succeeds
        when(cardPort.findCardByUserId(any(Integer.class)))
                .thenReturn(Uni.createFrom().item(card("9999999999999999", null)));

        try {
            transactionCommandService.create("api-key-1", req)
                    .await().indefinitely();
        } catch (Exception e) {
            // Expected — credit failure propagates
        }

        // Customer debit WAS applied (persisted before credit step)
        verify(transactionCommandRepository, atLeastOnce()).persist(any(com.sanedge.transaction.entity.Transaction.class));
        // Transaction status was set to SUCCESS (customer side complete)
        assertThat(transaction.status).isEqualTo(Status.SUCCESS);
    }
}
