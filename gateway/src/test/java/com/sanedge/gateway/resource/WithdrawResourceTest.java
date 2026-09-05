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

import com.sanedge.gateway.dto.WithdrawDto;
import com.sanedge.gateway.service.WithdrawService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class WithdrawResourceTest {

    @Mock private WithdrawService withdrawService;
    private WithdrawResource withdrawResource;

    @BeforeEach
    void setUp() throws Exception {
        withdrawResource = new WithdrawResource();
        Field f = WithdrawResource.class.getDeclaredField("withdrawService");
        f.setAccessible(true);
        f.set(withdrawResource, withdrawService);
    }

    @Test
    void listWithdraws_Success() {
        WithdrawDto.FindAllWithdrawResponse dto = new WithdrawDto.FindAllWithdrawResponse(List.of(), "success", "ok");
        lenient().when(withdrawService.listWithdraws(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        WithdrawDto.FindAllWithdrawResponse result = withdrawResource.listWithdraws(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getWithdraw_Success() {
        WithdrawDto.FindByIdWithdrawResponse dto = new WithdrawDto.FindByIdWithdrawResponse(null, "success", "ok");
        lenient().when(withdrawService.getWithdraw(anyInt())).thenReturn(Uni.createFrom().item(dto));
        WithdrawDto.FindByIdWithdrawResponse result = withdrawResource.getWithdraw(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createWithdraw_Success() {
        WithdrawDto.CreateWithdrawResponse dto = new WithdrawDto.CreateWithdrawResponse(null, "success", "created");
        lenient().when(withdrawService.createWithdraw(any())).thenReturn(Uni.createFrom().item(dto));
        WithdrawDto.CreateWithdrawResponse result = withdrawResource.createWithdraw(new WithdrawDto.CreateWithdrawBody("1234567890", 50000)).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void findMonthlyAmounts_Success() {
        WithdrawDto.ApiResponseWithdrawMonthAmount dto = new WithdrawDto.ApiResponseWithdrawMonthAmount("success", "ok", List.of());
        lenient().when(withdrawService.findMonthlyAmounts(anyInt())).thenReturn(Uni.createFrom().item(dto));
        WithdrawDto.ApiResponseWithdrawMonthAmount result = withdrawResource.findMonthlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
