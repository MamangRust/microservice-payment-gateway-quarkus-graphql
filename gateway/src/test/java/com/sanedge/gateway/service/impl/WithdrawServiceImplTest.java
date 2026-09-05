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

import com.sanedge.gateway.dto.WithdrawDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class WithdrawServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.withdraw.MutinyWithdrawQueryServiceGrpc.MutinyWithdrawQueryServiceStub withdrawQueryService;

    @Mock
    pb.withdraw.MutinyWithdrawCommandServiceGrpc.MutinyWithdrawCommandServiceStub withdrawCommandService;

    @Mock
    pb.withdraw.stats.MutinyWithdrawStatsAmountServiceGrpc.MutinyWithdrawStatsAmountServiceStub withdrawStatsAmountService;

    @Mock
    pb.withdraw.stats.MutinyWithdrawStatsStatusServiceGrpc.MutinyWithdrawStatsStatusServiceStub withdrawStatsStatusService;

    WithdrawServiceImpl withdrawService;

    @BeforeEach
    void setUp() throws Exception {
        withdrawService = new WithdrawServiceImpl();

        setField(withdrawService, "telemetryHelper", telemetryHelper);
        setField(withdrawService, "withdrawQueryService", withdrawQueryService);
        setField(withdrawService, "withdrawCommandService", withdrawCommandService);
        setField(withdrawService, "withdrawStatsAmountService", withdrawStatsAmountService);
        setField(withdrawService, "withdrawStatsStatusService", withdrawStatsStatusService);

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
    void listWithdraws_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponse protoWithdraw = pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
                .setWithdrawId(1)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(50000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw responseProto = pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw.newBuilder()
                .addData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraws found")
                .build();

        when(withdrawQueryService.findAllWithdraw(any(pb.withdraw.Withdraw.FindAllWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllWithdrawResponse result = withdrawService.listWithdraws(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getWithdraw_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponse protoWithdraw = pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
                .setWithdrawId(1)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(50000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.withdraw.Withdraw.ApiResponseWithdraw responseProto = pb.withdraw.Withdraw.ApiResponseWithdraw.newBuilder()
                .setData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraw found")
                .build();

        when(withdrawQueryService.findByIdWithdraw(any(pb.withdraw.Withdraw.FindByIdWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdWithdrawResponse result = withdrawService.getWithdraw(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().withdrawId()).isEqualTo(1);
        assertThat(result.data().withdrawAmount()).isEqualTo(50000);
    }

    @Test
    void createWithdraw_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponse protoWithdraw = pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
                .setWithdrawId(2)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(100000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.withdraw.Withdraw.ApiResponseWithdraw responseProto = pb.withdraw.Withdraw.ApiResponseWithdraw.newBuilder()
                .setData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraw created")
                .build();

        when(withdrawCommandService.createWithdraw(any(pb.withdraw.WithdrawCommand.CreateWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateWithdrawBody request = new CreateWithdrawBody("4111111111111111", 100000);
        CreateWithdrawResponse result = withdrawService.createWithdraw(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().withdrawAmount()).isEqualTo(100000);
    }

    @Test
    void updateWithdraw_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponse protoWithdraw = pb.withdraw.Withdraw.WithdrawResponse.newBuilder()
                .setWithdrawId(1)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(150000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.withdraw.Withdraw.ApiResponseWithdraw responseProto = pb.withdraw.Withdraw.ApiResponseWithdraw.newBuilder()
                .setData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraw updated")
                .build();

        when(withdrawCommandService.updateWithdraw(any(pb.withdraw.WithdrawCommand.UpdateWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateWithdrawBody request = new CreateWithdrawBody("4111111111111111", 150000);
        UpdateWithdrawResponse result = withdrawService.updateWithdraw(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().withdrawAmount()).isEqualTo(150000);
    }

    @Test
    void deleteWithdraw_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponseDeleteAt protoWithdraw = pb.withdraw.Withdraw.WithdrawResponseDeleteAt.newBuilder()
                .setWithdrawId(1)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(50000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt responseProto = pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt.newBuilder()
                .setData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraw deleted")
                .build();

        when(withdrawCommandService.trashedWithdraw(any(pb.withdraw.Withdraw.FindByIdWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedWithdrawResponse result = withdrawService.deleteWithdraw(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().withdrawId()).isEqualTo(1);
    }

    @Test
    void restoreWithdraw_returnsSuccess() {
        pb.withdraw.Withdraw.WithdrawResponseDeleteAt protoWithdraw = pb.withdraw.Withdraw.WithdrawResponseDeleteAt.newBuilder()
                .setWithdrawId(1)
                .setCardNumber("4111111111111111")
                .setWithdrawAmount(50000)
                .setWithdrawTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt responseProto = pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt.newBuilder()
                .setData(protoWithdraw)
                .setStatus("success")
                .setMessage("Withdraw restored")
                .build();

        when(withdrawCommandService.restoreWithdraw(any(pb.withdraw.Withdraw.FindByIdWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedWithdrawResponse result = withdrawService.restoreWithdraw(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteWithdrawPermanent_returnsSuccess() {
        pb.withdraw.WithdrawCommand.ApiResponseWithdrawDelete responseProto = pb.withdraw.WithdrawCommand.ApiResponseWithdrawDelete.newBuilder()
                .setStatus("success")
                .setMessage("Withdraw permanently deleted")
                .build();

        when(withdrawCommandService.deleteWithdrawPermanent(any(pb.withdraw.Withdraw.FindByIdWithdrawRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = withdrawService.deleteWithdrawPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Withdraw permanently deleted");
    }
}