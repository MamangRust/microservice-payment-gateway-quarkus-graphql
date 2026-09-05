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

import com.sanedge.gateway.dto.TransferDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.transfer.MutinyTransferQueryServiceGrpc.MutinyTransferQueryServiceStub transferQueryService;

    @Mock
    pb.transfer.MutinyTransferCommandServiceGrpc.MutinyTransferCommandServiceStub transferCommandService;

    @Mock
    pb.transfer.stats.MutinyTransferStatsAmountServiceGrpc.MutinyTransferStatsAmountServiceStub transferStatsAmountService;

    @Mock
    pb.transfer.stats.MutinyTransferStatsStatusServiceGrpc.MutinyTransferStatsStatusServiceStub transferStatsStatusService;

    TransferServiceImpl transferService;

    @BeforeEach
    void setUp() throws Exception {
        transferService = new TransferServiceImpl();

        setField(transferService, "telemetryHelper", telemetryHelper);
        setField(transferService, "transferQueryService", transferQueryService);
        setField(transferService, "transferCommandService", transferCommandService);
        setField(transferService, "transferStatsAmountService", transferStatsAmountService);
        setField(transferService, "transferStatsStatusService", transferStatsStatusService);

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
    void listTransfers_returnsSuccess() {
        pb.transfer.Transfer.TransferResponse protoTransfer = pb.transfer.Transfer.TransferResponse.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(100000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transfer.TransferQuery.ApiResponsePaginationTransfer responseProto = pb.transfer.TransferQuery.ApiResponsePaginationTransfer.newBuilder()
                .addData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfers found")
                .build();

        when(transferQueryService.findAllTransfer(any(pb.transfer.Transfer.FindAllTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllTransferResponse result = transferService.listTransfers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).transferFrom()).isEqualTo("4111111111111111");
    }

    @Test
    void getTransfer_returnsSuccess() {
        pb.transfer.Transfer.TransferResponse protoTransfer = pb.transfer.Transfer.TransferResponse.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(100000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transfer.Transfer.ApiResponseTransfer responseProto = pb.transfer.Transfer.ApiResponseTransfer.newBuilder()
                .setData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfer found")
                .build();

        when(transferQueryService.findByIdTransfer(any(pb.transfer.Transfer.FindByIdTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdTransferResponse result = transferService.getTransfer(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().transferAmount()).isEqualTo(100000);
    }

    @Test
    void createTransfer_returnsSuccess() {
        pb.transfer.Transfer.TransferResponse protoTransfer = pb.transfer.Transfer.TransferResponse.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(100000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.transfer.Transfer.ApiResponseTransfer responseProto = pb.transfer.Transfer.ApiResponseTransfer.newBuilder()
                .setData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfer created")
                .build();

        when(transferCommandService.createTransfer(any(pb.transfer.TransferCommand.CreateTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateTransferRequest request = new CreateTransferRequest("4111111111111111", "5222222222222222", 100000);
        CreateTransferResponse result = transferService.createTransfer(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().transferAmount()).isEqualTo(100000);
    }

    @Test
    void updateTransfer_returnsSuccess() {
        pb.transfer.Transfer.TransferResponse protoTransfer = pb.transfer.Transfer.TransferResponse.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(200000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transfer.Transfer.ApiResponseTransfer responseProto = pb.transfer.Transfer.ApiResponseTransfer.newBuilder()
                .setData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfer updated")
                .build();

        when(transferCommandService.updateTransfer(any(pb.transfer.TransferCommand.UpdateTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateTransferRequest request = new UpdateTransferRequest("4111111111111111", "5222222222222222", 200000);
        UpdateTransferResponse result = transferService.updateTransfer(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().transferAmount()).isEqualTo(200000);
    }

    @Test
    void trashTransfer_returnsSuccess() {
        pb.transfer.Transfer.TransferResponseDeleteAt protoTransfer = pb.transfer.Transfer.TransferResponseDeleteAt.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(100000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transfer.Transfer.ApIResponseTransferDeleteAt responseProto = pb.transfer.Transfer.ApIResponseTransferDeleteAt.newBuilder()
                .setData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfer trashed")
                .build();

        when(transferCommandService.trashedTransfer(any(pb.transfer.Transfer.FindByIdTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTransferResponse result = transferService.trashTransfer(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreTransfer_returnsSuccess() {
        pb.transfer.Transfer.TransferResponseDeleteAt protoTransfer = pb.transfer.Transfer.TransferResponseDeleteAt.newBuilder()
                .setId(1)
                .setTransferNo("TRF-001")
                .setTransferFrom("4111111111111111")
                .setTransferTo("5222222222222222")
                .setTransferAmount(100000)
                .setTransferTime("2024-01-01T00:00:00Z")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.transfer.Transfer.ApIResponseTransferDeleteAt responseProto = pb.transfer.Transfer.ApIResponseTransferDeleteAt.newBuilder()
                .setData(protoTransfer)
                .setStatus("success")
                .setMessage("Transfer restored")
                .build();

        when(transferCommandService.restoreTransfer(any(pb.transfer.Transfer.FindByIdTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedTransferResponse result = transferService.restoreTransfer(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteTransferPermanent_returnsSuccess() {
        pb.transfer.TransferCommand.ApiResponseTransferDelete responseProto = pb.transfer.TransferCommand.ApiResponseTransferDelete.newBuilder()
                .setStatus("success")
                .setMessage("Transfer permanently deleted")
                .build();

        when(transferCommandService.deleteTransferPermanent(any(pb.transfer.Transfer.FindByIdTransferRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = transferService.deleteTransferPermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Transfer permanently deleted");
    }
}