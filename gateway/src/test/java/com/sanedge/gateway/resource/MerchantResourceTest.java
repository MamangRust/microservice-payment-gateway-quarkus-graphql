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

import com.sanedge.gateway.dto.MerchantDto;
import com.sanedge.gateway.service.MerchantService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantResourceTest {

    @Mock private MerchantService merchantService;
    private MerchantResource merchantResource;

    @BeforeEach
    void setUp() throws Exception {
        merchantResource = new MerchantResource();
        Field f = MerchantResource.class.getDeclaredField("merchantService");
        f.setAccessible(true);
        f.set(merchantResource, merchantService);
    }

    @Test
    void listMerchants_Success() {
        MerchantDto.FindAllMerchantResponse dto = new MerchantDto.FindAllMerchantResponse(List.of(), "success", "ok");
        lenient().when(merchantService.listMerchants(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        MerchantDto.FindAllMerchantResponse result = merchantResource.listMerchants(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getMerchant_Success() {
        MerchantDto.FindByIdMerchantResponse dto = new MerchantDto.FindByIdMerchantResponse(null, "success", "ok");
        lenient().when(merchantService.getMerchant(anyInt())).thenReturn(Uni.createFrom().item(dto));
        MerchantDto.FindByIdMerchantResponse result = merchantResource.getMerchant(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createMerchant_Success() {
        MerchantDto.CreateMerchantResponse dto = new MerchantDto.CreateMerchantResponse(null, "success", "created");
        lenient().when(merchantService.createMerchant(any())).thenReturn(Uni.createFrom().item(dto));
        MerchantDto.CreateMerchantResponse result = merchantResource.createMerchant(new MerchantDto.CreateMerchantRequest(1, "Test")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void getMonthlyAmount_Success() {
        MerchantDto.ApiResponseMerchantMonthlyAmount dto = new MerchantDto.ApiResponseMerchantMonthlyAmount("success", "ok", List.of());
        lenient().when(merchantService.getMonthlyAmount(anyInt())).thenReturn(Uni.createFrom().item(dto));
        MerchantDto.ApiResponseMerchantMonthlyAmount result = merchantResource.getMonthlyAmount(2024).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
