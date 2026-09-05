package com.sanedge.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transaction.Transaction;
import pb.transaction.TransactionQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class TransactionQueryGrpcHandlerTest {

        @Mock
        private TransactionQueryService transactionQueryService;

        private TransactionQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TransactionQueryGrpcHandler();
                handler.transactionQueryService = transactionQueryService;
        }

        // ---------- helpers ----------

        private TransactionResponse createTransactionResponse(Long id) {
                TransactionResponse r = new TransactionResponse();
                r.setId(id);
                r.setTransactionNo("TXN-20240615-001");
                r.setCardNumber("1234-5678-9012-3456");
                r.setAmount(150000L);
                r.setPaymentMethod("CREDIT");
                r.setMerchantId(1L);
                r.setTransactionTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                return r;
        }

        private TransactionResponseDeleteAt createTransactionResponseDeleteAt(Long id) {
                TransactionResponseDeleteAt r = new TransactionResponseDeleteAt();
                r.setId(id);
                r.setTransactionNo("TXN-20240615-001");
                r.setCardNumber("1234-5678-9012-3456");
                r.setAmount(150000L);
                r.setPaymentMethod("CREDIT");
                r.setMerchantId(1L);
                r.setTransactionTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0, 0).toString());
                return r;
        }

        // ========== findAllTransaction ==========

        @Test
        @DisplayName("findAllTransaction - success")
        void findAllTransaction_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).setSearch("").build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Transactions retrieved successfully", List.of(data), null);

                when(transactionQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionQuery.ApiResponsePaginationTransaction response = handler.findAllTransaction(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transactions retrieved successfully");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("findAllTransaction - internal error")
        void findAllTransaction_Error() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).build();

                when(transactionQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                try {
                        handler.findAllTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findAllTransactionByCardNumber ==========

        @Test
        @DisplayName("findAllTransactionByCardNumber - success")
        void findAllByCardNumber_Success() {
                TransactionQuery.FindAllTransactionCardNumberRequest request = TransactionQuery.FindAllTransactionCardNumberRequest
                                .newBuilder()
                                .setCardNumber("1234-5678-9012-3456")
                                .setPage(1).setPageSize(10).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Transactions retrieved successfully", List.of(data), null);

                when(transactionQueryService.findAllByCardNumber(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionQuery.ApiResponsePaginationTransaction response = handler
                                .findAllTransactionByCardNumber(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAllTransactionByCardNumber - internal error")
        void findAllByCardNumber_Error() {
                TransactionQuery.FindAllTransactionCardNumberRequest request = TransactionQuery.FindAllTransactionCardNumberRequest
                                .newBuilder().build();

                when(transactionQueryService.findAllByCardNumber(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                try {
                        handler.findAllTransactionByCardNumber(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByIdTransaction ==========

        @Test
        @DisplayName("findByIdTransaction - success")
        void findById_Success() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction retrieved successfully",
                                data);

                when(transactionQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.findByIdTransaction(request).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByIdTransaction - NOT_FOUND")
        void findById_NotFound() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(999).build();

                when(transactionQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Transaction not found")));

                try {
                        handler.findByIdTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("findByIdTransaction - internal error")
        void findById_Error() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                when(transactionQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Unexpected error")));

                try {
                        handler.findByIdTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findTransactionByMerchantId ==========

        @Test
        @DisplayName("findTransactionByMerchantId - success")
        void findByMerchantId_Success() {
                TransactionQuery.FindTransactionByMerchantIdRequest request = TransactionQuery.FindTransactionByMerchantIdRequest
                                .newBuilder().setMerchantId(1).build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponse<List<TransactionResponse>> apiResp = ApiResponse.success(
                                "Transactions retrieved successfully", List.of(data));

                when(transactionQueryService.findByMerchantId(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransactions response = handler.findTransactionByMerchantId(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findTransactionByMerchantId - internal error")
        void findByMerchantId_Error() {
                TransactionQuery.FindTransactionByMerchantIdRequest request = TransactionQuery.FindTransactionByMerchantIdRequest
                                .newBuilder().build();

                when(transactionQueryService.findByMerchantId(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                try {
                        handler.findTransactionByMerchantId(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByActiveTransaction ==========

        @Test
        @DisplayName("findByActiveTransaction - success")
        void findByActive_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                TransactionResponseDeleteAt data = createTransactionResponseDeleteAt(1L);
                ApiResponsePagination<List<TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active transactions retrieved successfully", List.of(data), null);

                when(transactionQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionQuery.ApiResponsePaginationTransactionDeleteAt response = handler
                                .findByActiveTransaction(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByActiveTransaction - internal error")
        void findByActive_Error() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .build();

                when(transactionQueryService.findByActive(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                try {
                        handler.findByActiveTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByTrashedTransaction ==========

        @Test
        @DisplayName("findByTrashedTransaction - success")
        void findByTrashed_Success() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).setPageSize(10).build();

                TransactionResponseDeleteAt data = createTransactionResponseDeleteAt(1L);
                ApiResponsePagination<List<TransactionResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed transactions retrieved successfully", List.of(data), null);

                when(transactionQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionQuery.ApiResponsePaginationTransactionDeleteAt response = handler
                                .findByTrashedTransaction(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByTrashedTransaction - internal error")
        void findByTrashed_Error() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .build();

                when(transactionQueryService.findByTrashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                try {
                        handler.findByTrashedTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== edge cases ==========

        @Test
        @DisplayName("findAllTransaction - empty result")
        void findAll_Empty() {
                TransactionQuery.FindAllTransactionRequest request = TransactionQuery.FindAllTransactionRequest
                                .newBuilder()
                                .setPage(1).build();

                ApiResponsePagination<List<TransactionResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "No transactions found", List.of(), null);

                when(transactionQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionQuery.ApiResponsePaginationTransaction response = handler.findAllTransaction(request)
                                .await().indefinitely();

                assertThat(response.getDataCount()).isZero();
        }

        @Test
        @DisplayName("findByIdTransaction - null data")
        void findById_NullData() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("No data", null);
                when(transactionQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.findByIdTransaction(request).await()
                                .indefinitely();

                assertThat(response.hasData()).isFalse();
        }
}