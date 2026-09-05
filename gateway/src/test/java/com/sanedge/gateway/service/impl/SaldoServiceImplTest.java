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

import com.sanedge.gateway.dto.SaldoDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class SaldoServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.saldo.MutinySaldoQueryServiceGrpc.MutinySaldoQueryServiceStub saldoQueryService;

    @Mock
    pb.saldo.MutinySaldoCommandServiceGrpc.MutinySaldoCommandServiceStub saldoCommandService;

    SaldoServiceImpl saldoService;

    @BeforeEach
    void setUp() throws Exception {
        saldoService = new SaldoServiceImpl();

        setField(saldoService, "telemetryHelper", telemetryHelper);
        setField(saldoService, "saldoQueryService", saldoQueryService);
        setField(saldoService, "saldoCommandService", saldoCommandService);

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
    void listSaldos_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponse protoSaldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(100000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.saldo.SaldoQuery.ApiResponsePaginationSaldo responseProto = pb.saldo.SaldoQuery.ApiResponsePaginationSaldo.newBuilder()
                .addData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldos found")
                .build();

        when(saldoQueryService.findAllSaldo(any(pb.saldo.Saldo.FindAllSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllSaldoResponse result = saldoService.listSaldos(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void getSaldo_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponse protoSaldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(100000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldo responseProto = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo found")
                .build();

        when(saldoQueryService.findByIdSaldo(any(pb.saldo.Saldo.FindByIdSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdSaldoResponse result = saldoService.getSaldo(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().saldoId()).isEqualTo(1);
        assertThat(result.data().totalBalance()).isEqualTo(100000);
    }

    @Test
    void findSaldoByCard_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponse protoSaldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(100000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldo responseProto = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo found")
                .build();

        when(saldoQueryService.findByCardNumber(any(pb.card.Card.FindByCardNumberRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdSaldoResponse result = saldoService.findSaldoByCard("4111111111111111").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void createSaldo_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponse protoSaldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(50000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldo responseProto = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo created")
                .build();

        when(saldoCommandService.createSaldo(any(pb.saldo.SaldoCommand.CreateSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateSaldoRequest request = new CreateSaldoRequest("4111111111111111", 50000);
        CreateSaldoResponse result = saldoService.createSaldo(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().totalBalance()).isEqualTo(50000);
    }

    @Test
    void updateSaldoBalance_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponse protoSaldo = pb.saldo.Saldo.SaldoResponse.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(150000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldo responseProto = pb.saldo.Saldo.ApiResponseSaldo.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo balance updated")
                .build();

        when(saldoCommandService.updateSaldoBalance(any(pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateSaldoBalanceRequest request = new UpdateSaldoBalanceRequest("4111111111111111", 150000);
        UpdateSaldoResponse result = saldoService.updateSaldoBalance(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().totalBalance()).isEqualTo(150000);
    }

    @Test
    void deleteSaldo_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponseDeleteAt protoSaldo = pb.saldo.Saldo.SaldoResponseDeleteAt.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(100000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldoDeleteAt responseProto = pb.saldo.Saldo.ApiResponseSaldoDeleteAt.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo deleted")
                .build();

        when(saldoCommandService.trashedSaldo(any(pb.saldo.Saldo.FindByIdSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedSaldoResponse result = saldoService.deleteSaldo(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().saldoId()).isEqualTo(1);
    }

    @Test
    void restoreSaldo_returnsSuccess() {
        pb.saldo.Saldo.SaldoResponseDeleteAt protoSaldo = pb.saldo.Saldo.SaldoResponseDeleteAt.newBuilder()
                .setSaldoId(1)
                .setCardNumber("4111111111111111")
                .setTotalBalance(100000)
                .setWithdrawTime("")
                .setWithdrawAmount(0)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.saldo.Saldo.ApiResponseSaldoDeleteAt responseProto = pb.saldo.Saldo.ApiResponseSaldoDeleteAt.newBuilder()
                .setData(protoSaldo)
                .setStatus("success")
                .setMessage("Saldo restored")
                .build();

        when(saldoCommandService.restoreSaldo(any(pb.saldo.Saldo.FindByIdSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedSaldoResponse result = saldoService.restoreSaldo(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteSaldoPermanent_returnsSuccess() {
        pb.saldo.SaldoCommand.ApiResponseSaldoDelete responseProto = pb.saldo.SaldoCommand.ApiResponseSaldoDelete.newBuilder()
                .setStatus("success")
                .setMessage("Saldo permanently deleted")
                .build();

        when(saldoCommandService.deleteSaldoPermanent(any(pb.saldo.Saldo.FindByIdSaldoRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = saldoService.deleteSaldoPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Saldo permanently deleted");
    }

    @Test
    void restoreAllSaldos_returnsSuccess() {
        pb.saldo.SaldoCommand.ApiResponseSaldoAll responseProto = pb.saldo.SaldoCommand.ApiResponseSaldoAll.newBuilder()
                .setStatus("success")
                .setMessage("All saldos restored")
                .build();

        when(saldoCommandService.restoreAllSaldo(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = saldoService.restoreAllSaldos().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All saldos restored");
    }
}