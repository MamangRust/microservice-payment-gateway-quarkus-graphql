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

import com.sanedge.gateway.dto.TopupDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TopupServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.topup.MutinyTopupQueryServiceGrpc.MutinyTopupQueryServiceStub topupQueryService;

    @Mock
    pb.topup.MutinyTopupCommandServiceGrpc.MutinyTopupCommandServiceStub topupCommandService;

    @Mock
    pb.topup.stats.MutinyTopupStatsAmountServiceGrpc.MutinyTopupStatsAmountServiceStub topupStatsAmountService;

    @Mock
    pb.topup.stats.MutinyTopupStatsMethodServiceGrpc.MutinyTopupStatsMethodServiceStub topupStatsMethodService;

    @Mock
    pb.topup.stats.MutinyTopupStatsStatusServiceGrpc.MutinyTopupStatsStatusServiceStub topupStatsStatusService;

    TopupServiceImpl topupService;

    @BeforeEach
    void setUp() throws Exception {
        topupService = new TopupServiceImpl();

        setField(topupService, "telemetryHelper", telemetryHelper);
        setField(topupService, "topupQueryService", topupQueryService);
        setField(topupService, "topupCommandService", topupCommandService);
        setField(topupService, "topupStatsAmountService", topupStatsAmountService);
        setField(topupService, "topupStatsMethodService", topupStatsMethodService);
        setField(topupService, "topupStatsStatusService", topupStatsStatusService);

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
    void listTopups_returnsSuccess() {
        pb.topup.Topup.TopupResponse protoTopup = pb.topup.Topup.TopupResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(50000)
                .setTopupMethod("BANK_TRANSFER")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.topup.TopupQuery.ApiResponsePaginationTopup responseProto = pb.topup.TopupQuery.ApiResponsePaginationTopup.newBuilder()
                .addData(protoTopup)
                .setStatus("success")
                .setMessage("Topups found")
                .build();

        when(topupQueryService.findAllTopup(any(pb.topup.TopupQuery.FindAllTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllTopupResponse result = topupService.listTopups(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).topupAmount()).isEqualTo(50000);
    }

    @Test
    void getTopup_returnsSuccess() {
        pb.topup.Topup.TopupResponse protoTopup = pb.topup.Topup.TopupResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(50000)
                .setTopupMethod("BANK_TRANSFER")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.topup.Topup.ApiResponseTopup responseProto = pb.topup.Topup.ApiResponseTopup.newBuilder()
                .setData(protoTopup)
                .setStatus("success")
                .setMessage("Topup found")
                .build();

        when(topupQueryService.findByIdTopup(any(pb.topup.Topup.FindByIdTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdTopupResponse result = topupService.getTopup(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().topupAmount()).isEqualTo(50000);
    }

    @Test
    void createTopup_returnsSuccess() {
        pb.topup.Topup.TopupResponse protoTopup = pb.topup.Topup.TopupResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(50000)
                .setTopupMethod("BANK_TRANSFER")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.topup.Topup.ApiResponseTopup responseProto = pb.topup.Topup.ApiResponseTopup.newBuilder()
                .setData(protoTopup)
                .setStatus("success")
                .setMessage("Topup created")
                .build();

        when(topupCommandService.createTopup(any(pb.topup.TopupCommand.CreateTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateTopupRequest request = new CreateTopupRequest("4111111111111111", 50000, "BANK_TRANSFER");
        CreateTopupResponse result = topupService.createTopup(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().topupAmount()).isEqualTo(50000);
    }

    @Test
    void updateTopup_returnsSuccess() {
        pb.topup.Topup.TopupResponse protoTopup = pb.topup.Topup.TopupResponse.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(100000)
                .setTopupMethod("CREDIT_CARD")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.topup.Topup.ApiResponseTopup responseProto = pb.topup.Topup.ApiResponseTopup.newBuilder()
                .setData(protoTopup)
                .setStatus("success")
                .setMessage("Topup updated")
                .build();

        when(topupCommandService.updateTopup(any(pb.topup.TopupCommand.UpdateTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateTopupRequest request = new UpdateTopupRequest("4111111111111111", 100000, "CREDIT_CARD");
        UpdateTopupResponse result = topupService.updateTopup(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().topupAmount()).isEqualTo(100000);
    }

    @Test
    void trashTopup_returnsSuccess() {
        pb.topup.Topup.TopupResponseDeleteAt protoTopup = pb.topup.Topup.TopupResponseDeleteAt.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(50000)
                .setTopupMethod("BANK_TRANSFER")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.topup.Topup.ApiResponseTopupDeleteAt responseProto = pb.topup.Topup.ApiResponseTopupDeleteAt.newBuilder()
                .setData(protoTopup)
                .setStatus("success")
                .setMessage("Topup trashed")
                .build();

        when(topupCommandService.trashedTopup(any(pb.topup.Topup.FindByIdTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTopupResponse result = topupService.trashTopup(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreTopup_returnsSuccess() {
        pb.topup.Topup.TopupResponseDeleteAt protoTopup = pb.topup.Topup.TopupResponseDeleteAt.newBuilder()
                .setId(1)
                .setCardNumber("4111111111111111")
                .setTopupNo("TUP-001")
                .setTopupAmount(50000)
                .setTopupMethod("BANK_TRANSFER")
                .setTopupTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.topup.Topup.ApiResponseTopupDeleteAt responseProto = pb.topup.Topup.ApiResponseTopupDeleteAt.newBuilder()
                .setData(protoTopup)
                .setStatus("success")
                .setMessage("Topup restored")
                .build();

        when(topupCommandService.restoreTopup(any(pb.topup.Topup.FindByIdTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTopupResponse result = topupService.restoreTopup(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteTopupPermanent_returnsSuccess() {
        pb.topup.TopupCommand.ApiResponseTopupDelete responseProto = pb.topup.TopupCommand.ApiResponseTopupDelete.newBuilder()
                .setStatus("success")
                .setMessage("Topup permanently deleted")
                .build();

        when(topupCommandService.deleteTopupPermanent(any(pb.topup.Topup.FindByIdTopupRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = topupService.deleteTopupPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Topup permanently deleted");
    }

    @Test
    void restoreAllTopups_returnsSuccess() {
        pb.topup.TopupCommand.ApiResponseTopupAll responseProto = pb.topup.TopupCommand.ApiResponseTopupAll.newBuilder()
                .setStatus("success")
                .setMessage("All topups restored")
                .build();

        when(topupCommandService.restoreAllTopup(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = topupService.restoreAllTopups().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All topups restored");
    }
}