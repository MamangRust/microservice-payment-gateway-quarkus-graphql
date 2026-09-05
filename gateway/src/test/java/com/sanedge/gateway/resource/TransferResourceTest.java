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

import com.sanedge.gateway.dto.TransferDto;
import com.sanedge.gateway.service.TransferService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransferResourceTest {

    @Mock private TransferService transferService;
    private TransferResource transferResource;

    @BeforeEach
    void setUp() throws Exception {
        transferResource = new TransferResource();
        Field f = TransferResource.class.getDeclaredField("transferService");
        f.setAccessible(true);
        f.set(transferResource, transferService);
    }

    @Test
    void listTransfers_Success() {
        TransferDto.FindAllTransferResponse dto = new TransferDto.FindAllTransferResponse(List.of(), "success", "ok");
        lenient().when(transferService.listTransfers(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        TransferDto.FindAllTransferResponse result = transferResource.listTransfers(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTransfer_Success() {
        TransferDto.FindByIdTransferResponse dto = new TransferDto.FindByIdTransferResponse(null, "success", "ok");
        lenient().when(transferService.getTransfer(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TransferDto.FindByIdTransferResponse result = transferResource.getTransfer(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createTransfer_Success() {
        TransferDto.CreateTransferResponse dto = new TransferDto.CreateTransferResponse(null, "success", "created");
        lenient().when(transferService.createTransfer(any())).thenReturn(Uni.createFrom().item(dto));
        TransferDto.CreateTransferResponse result = transferResource.createTransfer(new TransferDto.CreateTransferRequest("111", "222", 100000)).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void findMonthlyAmounts_Success() {
        TransferDto.ApiResponseTransferMonthAmount dto = new TransferDto.ApiResponseTransferMonthAmount("success", "ok", List.of());
        lenient().when(transferService.findMonthlyAmounts(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TransferDto.ApiResponseTransferMonthAmount result = transferResource.findMonthlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
