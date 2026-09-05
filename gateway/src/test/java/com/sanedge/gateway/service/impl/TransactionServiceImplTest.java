package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.TransactionDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.transaction.MutinyTransactionQueryServiceGrpc.MutinyTransactionQueryServiceStub transactionQueryService;

    @Mock
    pb.transaction.MutinyTransactionCommandServiceGrpc.MutinyTransactionCommandServiceStub transactionCommandService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc.MutinyTransactionStatsAmountServiceStub transactionStatsAmountService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc.MutinyTransactionStatsMethodServiceStub transactionStatsMethodService;

    @Mock
    pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc.MutinyTransactionStatsStatusServiceStub transactionStatsStatusService;

    TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() throws Exception {
        transactionService = new TransactionServiceImpl();

        setField(transactionService, "telemetryHelper", telemetryHelper);
        setField(transactionService, "transactionQueryService", transactionQueryService);
        setField(transactionService, "transactionCommandService", transactionCommandService);
        setField(transactionService, "transactionStatsAmountService", transactionStatsAmountService);
        setField(transactionService, "transactionStatsMethodService", transactionStatsMethodService);
        setField(transactionService, "transactionStatsStatusService", transactionStatsStatusService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listTransactions_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse protoTransaction = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setAmount(50000)
                .setPaymentMethod("CREDIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transaction.TransactionQuery.ApiResponsePaginationTransaction responseProto = pb.transaction.TransactionQuery.ApiResponsePaginationTransaction.newBuilder()
                .addData(protoTransaction)
                .setStatus("success")
                .setMessage("Transactions found")
                .build();

        when(transactionQueryService.findAllTransaction(any(pb.transaction.TransactionQuery.FindAllTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllTransactionResponse result = transactionService.listTransactions(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse protoTransaction = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setAmount(50000)
                .setPaymentMethod("CREDIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto = pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                .setData(protoTransaction)
                .setStatus("success")
                .setMessage("Transaction found")
                .build();

        when(transactionQueryService.findByIdTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdTransactionResponse result = transactionService.getTransaction(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().amount()).isEqualTo(50000);
    }

    @Test
    void createTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse protoTransaction = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(2)
                .setCardNumber("4111111111111111")
                .setAmount(100000)
                .setPaymentMethod("DEBIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto = pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                .setData(protoTransaction)
                .setStatus("success")
                .setMessage("Transaction created")
                .build();

        when(transactionCommandService.createTransaction(any(pb.transaction.TransactionCommand.CreateTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateTransactionBody request = new CreateTransactionBody("4111111111111111", 100000, "DEBIT_CARD", 1);
        CreateTransactionResponse result = transactionService.createTransaction(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().amount()).isEqualTo(100000);
    }

    @Test
    void updateTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponse protoTransaction = pb.transaction.Transaction.TransactionResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setAmount(150000)
                .setPaymentMethod("CREDIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transaction.Transaction.ApiResponseTransaction responseProto = pb.transaction.Transaction.ApiResponseTransaction.newBuilder()
                .setData(protoTransaction)
                .setStatus("success")
                .setMessage("Transaction updated")
                .build();

        when(transactionCommandService.updateTransaction(any(pb.transaction.TransactionCommand.UpdateTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateTransactionBody request = new UpdateTransactionBody("4111111111111111", 150000, "CREDIT_CARD", 1);
        UpdateTransactionResponse result = transactionService.updateTransaction(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().amount()).isEqualTo(150000);
    }

    @Test
    void trashTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt protoTransaction = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setAmount(50000)
                .setPaymentMethod("CREDIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transaction.Transaction.ApiResponseTransactionDeleteAt responseProto = pb.transaction.Transaction.ApiResponseTransactionDeleteAt.newBuilder()
                .setData(protoTransaction)
                .setStatus("success")
                .setMessage("Transaction trashed")
                .build();

        when(transactionCommandService.trashedTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTransactionResponse result = transactionService.trashTransaction(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreTransaction_returnsSuccess() {
        pb.transaction.Transaction.TransactionResponseDeleteAt protoTransaction = pb.transaction.Transaction.TransactionResponseDeleteAt.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setAmount(50000)
                .setPaymentMethod("CREDIT_CARD")
                .setMerchantId(1)
                .setTransactionTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transaction.Transaction.ApiResponseTransactionDeleteAt responseProto = pb.transaction.Transaction.ApiResponseTransactionDeleteAt.newBuilder()
                .setData(protoTransaction)
                .setStatus("success")
                .setMessage("Transaction restored")
                .build();

        when(transactionCommandService.restoreTransaction(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTransactionResponse result = transactionService.restoreTransaction(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteTransactionPermanent_returnsSuccess() {
        pb.transaction.TransactionCommand.ApiResponseTransactionDelete responseProto = pb.transaction.TransactionCommand.ApiResponseTransactionDelete.newBuilder()
                .setStatus("success")
                .setMessage("Transaction permanently deleted")
                .build();

        when(transactionCommandService.deleteTransactionPermanent(any(pb.transaction.Transaction.FindByIdTransactionRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = transactionService.deleteTransactionPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Transaction permanently deleted");
    }
}