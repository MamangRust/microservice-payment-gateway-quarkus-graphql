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

import com.sanedge.gateway.dto.MerchantDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.merchant.MutinyMerchantQueryServiceGrpc.MutinyMerchantQueryServiceStub merchantQueryService;

    @Mock
    pb.merchant.MutinyMerchantCommandServiceGrpc.MutinyMerchantCommandServiceStub merchantCommandService;

    @Mock
    pb.merchant.MutinyMerchantTransactionServiceGrpc.MutinyMerchantTransactionServiceStub merchantTransactionService;

    @Mock
    pb.merchant.stats.MutinyMerchantStatsAmountServiceGrpc.MutinyMerchantStatsAmountServiceStub merchantStatsAmountService;

    @Mock
    pb.merchant.stats.MutinyMerchantStatsMethodServiceGrpc.MutinyMerchantStatsMethodServiceStub merchantStatsMethodService;

    @Mock
    pb.merchant.stats.MutinyMerchantStatsTotalAmountServiceGrpc.MutinyMerchantStatsTotalAmountServiceStub merchantStatsTotalAmountService;

    MerchantServiceImpl merchantService;

    @BeforeEach
    void setUp() throws Exception {
        merchantService = new MerchantServiceImpl();

        setField(merchantService, "telemetryHelper", telemetryHelper);
        setField(merchantService, "merchantQueryService", merchantQueryService);
        setField(merchantService, "merchantCommandService", merchantCommandService);
        setField(merchantService, "merchantTransactionService", merchantTransactionService);
        setField(merchantService, "merchantStatsAmountService", merchantStatsAmountService);
        setField(merchantService, "merchantStatsMethodService", merchantStatsMethodService);
        setField(merchantService, "merchantStatsTotalAmountService", merchantStatsTotalAmountService);

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
    void listMerchants_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant.MerchantQuery.ApiResponsePaginationMerchant responseProto = pb.merchant.MerchantQuery.ApiResponsePaginationMerchant.newBuilder()
                .addData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchants found")
                .build();

        when(merchantQueryService.findAllMerchant(any(pb.merchant.Merchant.FindAllMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllMerchantResponse result = merchantService.listMerchants(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("Test Merchant");
    }

    @Test
    void getMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant found")
                .build();

        when(merchantQueryService.findByIdMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdMerchantResponse result = merchantService.getMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().name()).isEqualTo("Test Merchant");
    }

    @Test
    void getMerchantByApiKey_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant found")
                .build();

        when(merchantQueryService.findByApiKey(any(pb.merchant.Merchant.FindByApiKeyRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdMerchantResponse result = merchantService.getMerchantByApiKey("api-key-123").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().apiKey()).isEqualTo("api-key-123");
    }

    @Test
    void createMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(2)
                .setName("New Merchant")
                .setApiKey("new-api-key")
                .setStatus("PENDING")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant created")
                .build();

        when(merchantCommandService.createMerchant(any(pb.merchant.MerchantCommand.CreateMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateMerchantRequest request = new CreateMerchantRequest(1, "New Merchant");
        CreateMerchantResponse result = merchantService.createMerchant(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("New Merchant");
    }

    @Test
    void updateMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Updated Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant updated")
                .build();

        when(merchantCommandService.updateMerchant(any(pb.merchant.MerchantCommand.UpdateMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateMerchantRequest request = new UpdateMerchantRequest(1, "Updated Merchant", "ACTIVE");
        UpdateMerchantResponse result = merchantService.updateMerchant(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("Updated Merchant");
    }

    @Test
    void updateMerchantStatus_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponse protoMerchant = pb.merchant.Merchant.MerchantResponse.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("INACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchant responseProto = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant status updated")
                .build();

        when(merchantCommandService.updateMerchantStatus(any(pb.merchant.MerchantCommand.UpdateMerchantStatusRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateMerchantResponse result = merchantService.updateMerchantStatus(1, "INACTIVE").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().status()).isEqualTo("INACTIVE");
    }

    @Test
    void deleteMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt protoMerchant = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchantDeleteAt responseProto = pb.merchant.Merchant.ApiResponseMerchantDeleteAt.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant deleted")
                .build();

        when(merchantCommandService.trashedMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedMerchantResponse result = merchantService.deleteMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreMerchant_returnsSuccess() {
        pb.merchant.Merchant.MerchantResponseDeleteAt protoMerchant = pb.merchant.Merchant.MerchantResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("Test Merchant")
                .setApiKey("api-key-123")
                .setStatus("ACTIVE")
                .setUserId(1)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.merchant.Merchant.ApiResponseMerchantDeleteAt responseProto = pb.merchant.Merchant.ApiResponseMerchantDeleteAt.newBuilder()
                .setData(protoMerchant)
                .setStatus("success")
                .setMessage("Merchant restored")
                .build();

        when(merchantCommandService.restoreMerchant(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedMerchantResponse result = merchantService.restoreMerchant(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteMerchantPermanent_returnsSuccess() {
        pb.merchant.MerchantCommand.ApiResponseMerchantDelete responseProto = pb.merchant.MerchantCommand.ApiResponseMerchantDelete.newBuilder()
                .setStatus("success")
                .setMessage("Merchant permanently deleted")
                .build();

        when(merchantCommandService.deleteMerchantPermanent(any(pb.merchant.Merchant.FindByIdMerchantRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = merchantService.deleteMerchantPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Merchant permanently deleted");
    }

    @Test
    void restoreAllMerchants_returnsSuccess() {
        pb.merchant.MerchantCommand.ApiResponseMerchantAll responseProto = pb.merchant.MerchantCommand.ApiResponseMerchantAll.newBuilder()
                .setStatus("success")
                .setMessage("All merchants restored")
                .build();

        when(merchantCommandService.restoreAllMerchant(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = merchantService.restoreAllMerchants().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All merchants restored");
    }
}