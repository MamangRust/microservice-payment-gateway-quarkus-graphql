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

import com.sanedge.gateway.dto.TopupDto;
import com.sanedge.gateway.service.TopupService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TopupResourceTest {

    @Mock private TopupService topupService;
    private TopupResource topupResource;

    @BeforeEach
    void setUp() throws Exception {
        topupResource = new TopupResource();
        Field f = TopupResource.class.getDeclaredField("topupService");
        f.setAccessible(true);
        f.set(topupResource, topupService);
    }

    @Test
    void listTopups_Success() {
        TopupDto.FindAllTopupResponse dto = new TopupDto.FindAllTopupResponse(List.of(), "success", "ok");
        lenient().when(topupService.listTopups(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        TopupDto.FindAllTopupResponse result = topupResource.listTopups(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getTopup_Success() {
        TopupDto.FindByIdTopupResponse dto = new TopupDto.FindByIdTopupResponse(null, "success", "ok");
        lenient().when(topupService.getTopup(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TopupDto.FindByIdTopupResponse result = topupResource.getTopup(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createTopup_Success() {
        TopupDto.CreateTopupResponse dto = new TopupDto.CreateTopupResponse(null, "success", "created");
        lenient().when(topupService.createTopup(any())).thenReturn(Uni.createFrom().item(dto));
        TopupDto.CreateTopupResponse result = topupResource.createTopup(new TopupDto.CreateTopupRequest("1234567890", 50000, "BANK_TRANSFER")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void getMonthlyAmounts_Success() {
        TopupDto.ApiResponseTopupMonthAmount dto = new TopupDto.ApiResponseTopupMonthAmount("success", "ok", List.of());
        lenient().when(topupService.findMonthlyAmounts(anyInt())).thenReturn(Uni.createFrom().item(dto));
        TopupDto.ApiResponseTopupMonthAmount result = topupResource.findMonthlyAmounts(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
