package com.sanedge.transfer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sanedge.common.test.PanacheSessionPassthrough;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.service.TransferQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transfer.Transfer;
import pb.transfer.TransferQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class TransferQueryGrpcHandlerTest {

        @Mock
        private TransferQueryService transferQueryService;

        private TransferQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TransferQueryGrpcHandler();
                handler.transferQueryService = transferQueryService;
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

        // ========== findAllTransfer ==========

        @Test
        @DisplayName("findAllTransfer - should return pagination on success")
        void findAllTransfer_Success() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("")
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponsePagination<List<TransferResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Transfers retrieved successfully", List.of(data), null);

                when(transferQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponsePaginationTransfer response = handler.findAllTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transfers retrieved successfully");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAllTransfer - should return INTERNAL on failure")
        void findAllTransfer_InternalError() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                when(transferQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findAllTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== findByIdTransfer ==========

        @Test
        @DisplayName("findByIdTransfer - should return ApiResponseTransfer on success")
        void findByIdTransfer_Success() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Transfer retrieved successfully", data);

                when(transferQueryService.findById(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.findByIdTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getTransferNo()).isEqualTo("TRF-20240615-001");
        }

        @Test
        @DisplayName("findByIdTransfer - should return NOT_FOUND when transfer not found")
        void findByIdTransfer_NotFound() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(999)
                                .build();

                when(transferQueryService.findById(any()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Transfer not found")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByIdTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByIdTransfer - should return INTERNAL on unexpected error")
        void findByIdTransfer_InternalError() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                when(transferQueryService.findById(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Unexpected error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByIdTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== findTransferByTransferFrom ==========

        @Test
        @DisplayName("findTransferByTransferFrom - should return transfers on success")
        void findTransferByTransferFrom_Success() {
                Transfer.FindTransferByTransferFromRequest request = Transfer.FindTransferByTransferFromRequest
                                .newBuilder()
                                .setTransferFrom("1234567890123456")
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponse<List<TransferResponse>> apiResp = ApiResponse.success(
                                "Transfers retrieved successfully", List.of(data));

                when(transferQueryService.findByTransferFrom(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponseTransfers response = handler.findTransferByTransferFrom(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getTransferFrom()).isEqualTo("1234567890123456");
        }

        @Test
        @DisplayName("findTransferByTransferFrom - should return INTERNAL on failure")
        void findTransferByTransferFrom_InternalError() {
                Transfer.FindTransferByTransferFromRequest request = Transfer.FindTransferByTransferFromRequest
                                .newBuilder()
                                .setTransferFrom("1234567890123456")
                                .build();

                when(transferQueryService.findByTransferFrom(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findTransferByTransferFrom(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== findTransferByTransferTo ==========

        @Test
        @DisplayName("findTransferByTransferTo - should return transfers on success")
        void findTransferByTransferTo_Success() {
                Transfer.FindTransferByTransferToRequest request = Transfer.FindTransferByTransferToRequest.newBuilder()
                                .setTransferTo("6543210987654321")
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponse<List<TransferResponse>> apiResp = ApiResponse.success(
                                "Transfers retrieved successfully", List.of(data));

                when(transferQueryService.findByTransferTo(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponseTransfers response = handler.findTransferByTransferTo(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getTransferTo()).isEqualTo("6543210987654321");
        }

        @Test
        @DisplayName("findTransferByTransferTo - should return INTERNAL on failure")
        void findTransferByTransferTo_InternalError() {
                Transfer.FindTransferByTransferToRequest request = Transfer.FindTransferByTransferToRequest.newBuilder()
                                .setTransferTo("6543210987654321")
                                .build();

                when(transferQueryService.findByTransferTo(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findTransferByTransferTo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== findByActiveTransfer ==========

        @Test
        @DisplayName("findByActiveTransfer - should return active transfers on success")
        void findByActiveTransfer_Success() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                TransferResponseDeleteAt data = createTransferResponseDeleteAt(1L);
                ApiResponsePagination<List<TransferResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active transfers retrieved successfully", List.of(data), null);

                when(transferQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponsePaginationTransferDeleteAt response = handler.findByActiveTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByActiveTransfer - should return INTERNAL on failure")
        void findByActiveTransfer_InternalError() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                when(transferQueryService.findByActive(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByActiveTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== findByTrashedTransfer ==========

        @Test
        @DisplayName("findByTrashedTransfer - should return trashed transfers on success")
        void findByTrashedTransfer_Success() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                TransferResponseDeleteAt data = createTransferResponseDeleteAt(1L);
                ApiResponsePagination<List<TransferResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed transfers retrieved successfully", List.of(data), null);

                when(transferQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponsePaginationTransferDeleteAt response = handler.findByTrashedTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByTrashedTransfer - should return INTERNAL on failure")
        void findByTrashedTransfer_InternalError() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                when(transferQueryService.findByTrashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByTrashedTransfer(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        // ========== Edge Cases ==========

        @Test
        @DisplayName("findAllTransfer - should return empty list when no data")
        void findAllTransfer_EmptyResult() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .build();

                ApiResponsePagination<List<TransferResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "No transfers found", List.of(), null);

                when(transferQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponsePaginationTransfer response = handler.findAllTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("findByIdTransfer - should handle null data gracefully")
        void findByIdTransfer_NullData() {
                Transfer.FindByIdTransferRequest request = Transfer.FindByIdTransferRequest.newBuilder()
                                .setTransferId(1)
                                .build();

                ApiResponse<TransferResponse> apiResp = ApiResponse.success("Transfer not found", null);

                when(transferQueryService.findById(any())).thenReturn(Uni.createFrom().item(apiResp));

                Transfer.ApiResponseTransfer response = handler.findByIdTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("findAllTransfer - should pass search parameter correctly")
        void findAllTransfer_WithSearchParameter() {
                Transfer.FindAllTransferRequest request = Transfer.FindAllTransferRequest.newBuilder()
                                .setPage(1)
                                .setPageSize(10)
                                .setSearch("TRF-001")
                                .build();

                TransferResponse data = createTransferResponse(1L);
                ApiResponsePagination<List<TransferResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Transfers retrieved successfully", List.of(data), null);

                when(transferQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransferQuery.ApiResponsePaginationTransfer response = handler.findAllTransfer(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}