package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.TransactionDto;
import com.sanedge.gateway.service.TransactionService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionResourceTest {

    @Mock private TransactionService transactionService;
    private TransactionResource transactionResource;

    @BeforeEach
    void setUp() throws Exception {
        transactionResource = new TransactionResource();
        Field f = TransactionResource.class.getDeclaredField("transactionService");
        f.setAccessible(true);
        f.set(transactionResource, transactionService);
    }

    @Test
    void listTransactions_Success() {
        TransactionDto.FindAllTransactionResponse dto = new TransactionDto.FindAllTransactionResponse(List.of(), "success", "ok");
        lenient().when(transactionService.listTransactions(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        TransactionDto.FindAllTransactionResponse result = transactionResource.listTransactions(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTransaction_Success() {
        TransactionDto.FindByIdTransactionResponse dto = new TransactionDto.FindByIdTransactionResponse(null, "success", "ok");
        lenient().when(transactionService.getTransaction(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TransactionDto.FindByIdTransactionResponse result = transactionResource.getTransaction(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createTransaction_Success() {
        TransactionDto.CreateTransactionResponse dto = new TransactionDto.CreateTransactionResponse(null, "success", "created");
        lenient().when(transactionService.createTransaction(any())).thenReturn(Uni.createFrom().item(dto));
        TransactionDto.CreateTransactionResponse result = transactionResource.createTransaction(new TransactionDto.CreateTransactionBody("123", 50000, "CSH", 1)).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void findMonthlyAmounts_Success() {
        TransactionDto.ApiResponseTransactionMonthAmount dto = new TransactionDto.ApiResponseTransactionMonthAmount("success", "ok", List.of());
        lenient().when(transactionService.findMonthlyAmounts(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TransactionDto.ApiResponseTransactionMonthAmount result = transactionResource.findMonthlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
