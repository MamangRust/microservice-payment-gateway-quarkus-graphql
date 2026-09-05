package com.sanedge.transaction.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.transaction.Transaction;
import pb.transaction.TransactionCommand;

@ExtendWith(MockitoExtension.class)
class TransactionCommandGrpcHandlerTest {

        @Mock
        private TransactionCommandService transactionCommandService;

        private TransactionCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TransactionCommandGrpcHandler();
                handler.transactionCommandService = transactionCommandService;
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

        // ========== createTransaction ==========

        @Test
        @DisplayName("createTransaction - success")
        void create_Success() {
                TransactionCommand.CreateTransactionRequest request = TransactionCommand.CreateTransactionRequest
                                .newBuilder()
                                .setApiKey("valid-key")
                                .setCardNumber("1234-5678-9012-3456")
                                .setAmount(150000)
                                .setPaymentMethod("CREDIT")
                                .setMerchantId(1)
                                .build();

                TransactionResponse data = createTransactionResponse(1L);
                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction created successfully",
                                data);

                when(transactionCommandService.create(anyString(), any())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.createTransaction(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transaction created successfully");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getAmount()).isEqualTo(150000);
        }

        @Test
        @DisplayName("createTransaction - internal error")
        void create_Error() {
                TransactionCommand.CreateTransactionRequest request = TransactionCommand.CreateTransactionRequest
                                .newBuilder()
                                .build();

                when(transactionCommandService.create(anyString(), any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Insufficient balance")));

                try {
                        handler.createTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== updateTransaction ==========

        @Test
        @DisplayName("updateTransaction - success")
        void update_Success() {
                TransactionCommand.UpdateTransactionRequest request = TransactionCommand.UpdateTransactionRequest
                                .newBuilder()
                                .setApiKey("valid-key")
                                .setTransactionId(1)
                                .setCardNumber("1234-5678-9012-3456")
                                .setAmount(200000)
                                .setPaymentMethod("DEBIT")
                                .setMerchantId(1)
                                .build();

                TransactionResponse data = createTransactionResponse(1L);
                data.setAmount(200000L);
                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Transaction updated successfully",
                                data);

                when(transactionCommandService.update(anyString(), any())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.updateTransaction(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getAmount()).isEqualTo(200000);
        }

        @Test
        @DisplayName("updateTransaction - internal error")
        void update_Error() {
                TransactionCommand.UpdateTransactionRequest request = TransactionCommand.UpdateTransactionRequest
                                .newBuilder()
                                .build();

                when(transactionCommandService.update(anyString(), any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));

                try {
                        handler.updateTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== trashedTransaction ==========

        @Test
        @DisplayName("trashedTransaction - success")
        void trash_Success() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                TransactionResponseDeleteAt data = createTransactionResponseDeleteAt(1L);
                ApiResponse<TransactionResponseDeleteAt> apiResp = ApiResponse.success(
                                "Transaction moved to trash successfully", data);

                when(transactionCommandService.trashed(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransactionDeleteAt response = handler.trashedTransaction(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashedTransaction - internal error")
        void trash_Error() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .build();

                when(transactionCommandService.trashed(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));

                try {
                        handler.trashedTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreTransaction ==========

        @Test
        @DisplayName("restoreTransaction - success")
        void restore_Success() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                TransactionResponseDeleteAt data = createTransactionResponseDeleteAt(1L);
                ApiResponse<TransactionResponseDeleteAt> apiResp = ApiResponse.success(
                                "Transaction restored successfully", data);

                when(transactionCommandService.restore(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransactionDeleteAt response = handler.restoreTransaction(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transaction restored successfully");
        }

        @Test
        @DisplayName("restoreTransaction - internal error")
        void restore_Error() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .build();

                when(transactionCommandService.restore(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));

                try {
                        handler.restoreTransaction(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteTransactionPermanent ==========

        @Test
        @DisplayName("deleteTransactionPermanent - success")
        void deletePermanent_Success() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .setTransactionId(1).build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Transaction permanently deleted!", true);

                when(transactionCommandService.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommand.ApiResponseTransactionDelete response = handler.deleteTransactionPermanent(request)
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Transaction permanently deleted!");
        }

        @Test
        @DisplayName("deleteTransactionPermanent - internal error")
        void deletePermanent_Error() {
                Transaction.FindByIdTransactionRequest request = Transaction.FindByIdTransactionRequest.newBuilder()
                                .build();

                when(transactionCommandService.deletePermanent(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));

                try {
                        handler.deleteTransactionPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreAllTransaction ==========

        @Test
        @DisplayName("restoreAllTransaction - success")
        void restoreAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All transactions restored successfully!", true);
                when(transactionCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommand.ApiResponseTransactionAll response = handler
                                .restoreAllTransaction(Empty.getDefaultInstance())
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All transactions restored successfully!");
        }

        @Test
        @DisplayName("restoreAllTransaction - internal error")
        void restoreAll_Error() {
                when(transactionCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));

                try {
                        handler.restoreAllTransaction(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteAllTransactionPermanent ==========

        @Test
        @DisplayName("deleteAllTransactionPermanent - success")
        void deleteAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All transactions permanently deleted!", true);
                when(transactionCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                TransactionCommand.ApiResponseTransactionAll response = handler
                                .deleteAllTransactionPermanent(Empty.getDefaultInstance())
                                .await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All transactions permanently deleted!");
        }

        @Test
        @DisplayName("deleteAllTransactionPermanent - internal error")
        void deleteAll_Error() {
                when(transactionCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));

                try {
                        handler.deleteAllTransactionPermanent(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== edge cases ==========

        @Test
        @DisplayName("createTransaction - null data")
        void create_NullData() {
                TransactionCommand.CreateTransactionRequest request = TransactionCommand.CreateTransactionRequest
                                .newBuilder()
                                .setApiKey("key").build();

                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Created", null);
                when(transactionCommandService.create(anyString(), any())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.createTransaction(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateTransaction - null data")
        void update_NullData() {
                TransactionCommand.UpdateTransactionRequest request = TransactionCommand.UpdateTransactionRequest
                                .newBuilder()
                                .setApiKey("key").build();

                ApiResponse<TransactionResponse> apiResp = ApiResponse.success("Updated", null);
                when(transactionCommandService.update(anyString(), any())).thenReturn(Uni.createFrom().item(apiResp));

                Transaction.ApiResponseTransaction response = handler.updateTransaction(request).await().indefinitely();

                assertThat(response.hasData()).isFalse();
        }
}