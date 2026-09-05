package com.sanedge.transfer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.service.TransferCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transfer.Transfer;
import pb.transfer.TransferCommand;

@ExtendWith(MockitoExtension.class)
class TransferCommandGrpcHandlerTest {

        @Mock
        private TransferCommandService transferCommandService;

        private TransferCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TransferCommandGrpcHandler();
                handler.transferCommandService = transferCommandService;
        }

        // ========== Helper Methods ==========

        private TransferResponse createTransferResponse(Long id) {
                TransferResponse response = new TransferResponse();
                response.setId(id);
                response.setTransferNo("TRF-20240615-001");
                response.setTransferFrom("1234567890123456");
                response.setTransferTo("6543210987654321");
                response.setTransferAmount(500000L);
                response.setTransferTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                return response;
        }

        private TransferResponseDeleteAt createTransferResponseDeleteAt(Long id) {
                TransferResponseDeleteAt response = new TransferResponseDeleteAt();
                response.setId(id);
                response.setTransferNo("TRF-20240615-001");
                response.setTransferFrom("1234567890123456");
                response.setTransferTo("6543210987654321");
                response.setTransferAmount(500000L);
                response.setTransferTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0, 0).toString());
                return response;
        }

        // ========== createTransfer ==========

        @Test
        @DisplayName("createTransfer - should return created transfer on success")
        void createTransfer_Success() {
                TransferCommand.CreateTransferRequest request = TransferCommand.CreateTransferRequest.newBuilder()
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(500000)
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Transfer created successfully", data);

                when(transferCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.createTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transfer created successfully");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getTransferNo()).isEqualTo("TRF-20240615-001");
                assertThat(response.getData().getTransferAmount()).isEqualTo(500000);
        }

        @Test
        @DisplayName("createTransfer - should return INTERNAL on failure")
        void createTransfer_InternalError() {
                TransferCommand.CreateTransferRequest request = TransferCommand.CreateTransferRequest.newBuilder()
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(500000)
                                .build();

                when(transferCommandService.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Insufficient balance")));

                StatusRuntimeException ex = null;
                try {
                        handler.createTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== updateTransfer ==========

        @Test
        @DisplayName("updateTransfer - should return updated transfer on success")
        void updateTransfer_Success() {
                TransferCommand.UpdateTransferRequest request = TransferCommand.UpdateTransferRequest.newBuilder()
                                .setTransferId(1)
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(750000)
                                .build();

                TransferResponse data = createTransferResponse(1L);
                data.setTransferAmount(750000L);
                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Transfer updated successfully", data);

                when(transferCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.updateTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transfer updated successfully");
                assertThat(response.getData().getTransferAmount()).isEqualTo(750000);
        }

        @Test
        @DisplayName("updateTransfer - should return INTERNAL on failure")
        void updateTransfer_InternalError() {
                TransferCommand.UpdateTransferRequest request = TransferCommand.UpdateTransferRequest.newBuilder()
                                .setTransferId(1)
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(750000)
                                .build();

                when(transferCommandService.update(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.updateTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== trashedTransfer ==========

        @Test
        @DisplayName("trashedTransfer - should return trashed transfer on success")
        void trashedTransfer_Success() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                TransferResponseDeleteAt data = createTransferResponseDeleteAt(1L);
                ApiResponse<TransferResponseDeleteAt> apiResp = ApiResponse.success(
                                "Transfer moved to trash successfully", data);

                when(transferCommandService.trashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApIResponseTransferDeleteAt response = handler.trashedTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashedTransfer - should return INTERNAL on failure")
        void trashedTransfer_InternalError() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                when(transferCommandService.trashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.trashedTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== restoreTransfer ==========

        @Test
        @DisplayName("restoreTransfer - should return restored transfer on success")
        void restoreTransfer_Success() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                TransferResponseDeleteAt data = createTransferResponseDeleteAt(1L);
                ApiResponse<TransferResponseDeleteAt> apiResp = ApiResponse.success(
                                "Transfer restored successfully", data);

                when(transferCommandService.restore(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApIResponseTransferDeleteAt response = handler.restoreTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transfer restored successfully");
        }

        @Test
        @DisplayName("restoreTransfer - should return INTERNAL on failure")
        void restoreTransfer_InternalError() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                when(transferCommandService.restore(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.restoreTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== deleteTransferPermanent ==========

        @Test
        @DisplayName("deleteTransferPermanent - should return success message on success")
        void deleteTransferPermanent_Success() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Transfer permanently deleted!", true);

                when(transferCommandService.deletePermanent(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferCommand.ApiResponseTransferDelete response = handler.deleteTransferPermanent(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transfer permanently deleted!");
        }

        @Test
        @DisplayName("deleteTransferPermanent - should return INTERNAL on failure")
        void deleteTransferPermanent_InternalError() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                when(transferCommandService.deletePermanent(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.deleteTransferPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== restoreAllTransfer ==========

        @Test
        @DisplayName("restoreAllTransfer - should return success message on success")
        void restoreAllTransfer_Success() {
                Empty request = Empty.getDefaultInstance();

                ApiResponse<Boolean> apiResp = ApiResponse.success("All transfers restored successfully!", true);

                when(transferCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                TransferCommand.ApiResponseTransferAll response = handler.restoreAllTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All transfers restored successfully!");
        }

        @Test
        @DisplayName("restoreAllTransfer - should return INTERNAL on failure")
        void restoreAllTransfer_InternalError() {
                Empty request = Empty.getDefaultInstance();

                when(transferCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.restoreAllTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== deleteAllTransferPermanent ==========

        @Test
        @DisplayName("deleteAllTransferPermanent - should return success message on success")
        void deleteAllTransferPermanent_Success() {
                Empty request = Empty.getDefaultInstance();

                ApiResponse<Boolean> apiResp = ApiResponse.success("All transfers permanently deleted!", true);

                when(transferCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                TransferCommand.ApiResponseTransferAll response = handler.deleteAllTransferPermanent(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All transfers permanently deleted!");
        }

        @Test
        @DisplayName("deleteAllTransferPermanent - should return INTERNAL on failure")
        void deleteAllTransferPermanent_InternalError() {
                Empty request = Empty.getDefaultInstance();

                when(transferCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));

                StatusRuntimeException ex = null;
                try {
                        handler.deleteAllTransferPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== Edge Cases ==========

        @Test
        @DisplayName("createTransfer - should handle null data gracefully")
        void createTransfer_NullData() {
                TransferCommand.CreateTransferRequest request = TransferCommand.CreateTransferRequest.newBuilder()
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(500000)
                                .build();

                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Transfer created but no data", null);

                when(transferCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.createTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateTransfer - should handle null data gracefully")
        void updateTransfer_NullData() {
                TransferCommand.UpdateTransferRequest request = TransferCommand.UpdateTransferRequest.newBuilder()
                                .setTransferId(1)
                                .setTransferFrom("1234567890123456")
                                .setTransferTo("6543210987654321")
                                .setTransferAmount(750000)
                                .build();

                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Update failed silently", null);

                when(transferCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.updateTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }
}