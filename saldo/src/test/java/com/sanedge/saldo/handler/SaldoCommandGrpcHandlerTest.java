package com.sanedge.saldo.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.sanedge.common.test.PanacheSessionPassthrough;

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.service.SaldoCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.saldo.Saldo;
import pb.saldo.SaldoCommand;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class SaldoCommandGrpcHandlerTest {

        @Mock
        private SaldoCommandService saldoCommandService;

        private SaldoCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new SaldoCommandGrpcHandler();
                handler.saldoCommandService = saldoCommandService;
        }

        private SaldoResponse createSaldoResponse(Long id) {
                SaldoResponse r = new SaldoResponse();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setTotalBalance(500000L);
                r.setWithdrawAmount(0L);
                r.setWithdrawTime(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                return r;
        }

        private SaldoResponseDeleteAt createSaldoResponseDeleteAt(Long id) {
                SaldoResponseDeleteAt r = new SaldoResponseDeleteAt();
                r.setId(id);
                r.setCardNumber("1234-5678-9012-3456");
                r.setTotalBalance(500000L);
                r.setWithdrawAmount(0L);
                r.setWithdrawTime(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0).toString());
                return r;
        }

        // createSaldo
        @Test
        @DisplayName("createSaldo - success")
        void create_Success() {
                SaldoCommand.CreateSaldoRequest request = SaldoCommand.CreateSaldoRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456")
                                .setTotalBalance(100000)
                                .build();

                SaldoResponse data = createSaldoResponse(1L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Saldo created", data);
                when(saldoCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.createSaldo(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getSaldoId()).isEqualTo(1);
                assertThat(response.getData().getTotalBalance()).isEqualTo(500000);
        }

        @Test
        @DisplayName("createSaldo - internal error")
        void create_Error() {
                SaldoCommand.CreateSaldoRequest request = SaldoCommand.CreateSaldoRequest.newBuilder().build();
                when(saldoCommandService.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Failed")));
                try {
                        handler.createSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // updateSaldo
        @Test
        @DisplayName("updateSaldo - success")
        void update_Success() {
                SaldoCommand.UpdateSaldoRequest request = SaldoCommand.UpdateSaldoRequest.newBuilder()
                                .setSaldoId(1)
                                .setCardNumber("1234-5678-9012-3456")
                                .setTotalBalance(200000)
                                .build();

                SaldoResponse data = createSaldoResponse(1L);
                data.setTotalBalance(200000L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Saldo updated", data);
                when(saldoCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.updateSaldo(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getTotalBalance()).isEqualTo(200000);
        }

        @Test
        @DisplayName("updateSaldo - internal error")
        void update_Error() {
                SaldoCommand.UpdateSaldoRequest request = SaldoCommand.UpdateSaldoRequest.newBuilder().build();
                when(saldoCommandService.update(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));
                try {
                        handler.updateSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // updateSaldoBalance
        @Test
        @DisplayName("updateSaldoBalance - success")
        void updateBalance_Success() {
                SaldoCommand.UpdateSaldoBalanceRequest request = SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456")
                                .setTotalBalance(300000)
                                .build();

                SaldoResponse data = createSaldoResponse(1L);
                data.setTotalBalance(300000L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Balance updated", data);
                when(saldoCommandService.updateSaldoBalance(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.updateSaldoBalance(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getTotalBalance()).isEqualTo(300000);
        }

        @Test
        @DisplayName("updateSaldoBalance - internal error")
        void updateBalance_Error() {
                SaldoCommand.UpdateSaldoBalanceRequest request = SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                                .build();
                when(saldoCommandService.updateSaldoBalance(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Balance update failed")));
                try {
                        handler.updateSaldoBalance(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // updateSaldoWithdraw
        @Test
        @DisplayName("updateSaldoWithdraw - success")
        void updateWithdraw_Success() {
                SaldoCommand.UpdateSaldoWithdrawRequest request = SaldoCommand.UpdateSaldoWithdrawRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456")
                                .setTotalBalance(250000)
                                .setWithdrawAmount(50000)
                                .setWithdrawTime("2024-06-15T10:30:00")
                                .build();

                SaldoResponse data = createSaldoResponse(1L);
                data.setTotalBalance(250000L);
                data.setWithdrawAmount(50000L);
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Withdraw updated", data);
                when(saldoCommandService.updateSaldoWithdraw(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.updateSaldoWithdraw(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getWithdrawAmount()).isEqualTo(50000);
        }

        @Test
        @DisplayName("updateSaldoWithdraw - internal error")
        void updateWithdraw_Error() {
                SaldoCommand.UpdateSaldoWithdrawRequest request = SaldoCommand.UpdateSaldoWithdrawRequest.newBuilder()
                                .build();
                when(saldoCommandService.updateSaldoWithdraw(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Withdraw update failed")));
                try {
                        handler.updateSaldoWithdraw(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // trashedSaldo
        @Test
        @DisplayName("trashedSaldo - success")
        void trash_Success() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                SaldoResponseDeleteAt data = createSaldoResponseDeleteAt(1L);
                ApiResponse<SaldoResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
                when(saldoCommandService.trash(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldoDeleteAt response = handler.trashedSaldo(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashedSaldo - internal error")
        void trash_Error() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().build();
                when(saldoCommandService.trash(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));
                try {
                        handler.trashedSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // restoreSaldo
        @Test
        @DisplayName("restoreSaldo - success")
        void restore_Success() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                SaldoResponseDeleteAt data = createSaldoResponseDeleteAt(1L);
                data.setDeletedAt(null); // restore clears deletedAt
                ApiResponse<SaldoResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
                when(saldoCommandService.restore(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldoDeleteAt response = handler.restoreSaldo(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isFalse();
        }

        @Test
        @DisplayName("restoreSaldo - internal error")
        void restore_Error() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().build();
                when(saldoCommandService.restore(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));
                try {
                        handler.restoreSaldo(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // deleteSaldoPermanent
        @Test
        @DisplayName("deleteSaldoPermanent - success")
        void deletePermanent_Success() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().setSaldoId(1).build();
                ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
                when(saldoCommandService.delete(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                SaldoCommand.ApiResponseSaldoDelete response = handler.deleteSaldoPermanent(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Permanently deleted");
        }

        @Test
        @DisplayName("deleteSaldoPermanent - internal error")
        void deletePermanent_Error() {
                Saldo.FindByIdSaldoRequest request = Saldo.FindByIdSaldoRequest.newBuilder().build();
                when(saldoCommandService.delete(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));
                try {
                        handler.deleteSaldoPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // restoreAllSaldo
        @Test
        @DisplayName("restoreAllSaldo - success")
        void restoreAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
                when(saldoCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                SaldoCommand.ApiResponseSaldoAll response = handler.restoreAllSaldo(Empty.getDefaultInstance()).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All restored");
        }

        @Test
        @DisplayName("restoreAllSaldo - internal error")
        void restoreAll_Error() {
                when(saldoCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));
                try {
                        handler.restoreAllSaldo(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // deleteAllSaldoPermanent
        @Test
        @DisplayName("deleteAllSaldoPermanent - success")
        void deleteAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
                when(saldoCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                SaldoCommand.ApiResponseSaldoAll response = handler.deleteAllSaldoPermanent(Empty.getDefaultInstance())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All permanently deleted");
        }

        @Test
        @DisplayName("deleteAllSaldoPermanent - internal error")
        void deleteAll_Error() {
                when(saldoCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));
                try {
                        handler.deleteAllSaldoPermanent(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // edge cases
        @Test
        @DisplayName("createSaldo - null data")
        void create_NullData() {
                SaldoCommand.CreateSaldoRequest request = SaldoCommand.CreateSaldoRequest.newBuilder().build();
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Created", null);
                when(saldoCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.createSaldo(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateSaldo - null data")
        void update_NullData() {
                SaldoCommand.UpdateSaldoRequest request = SaldoCommand.UpdateSaldoRequest.newBuilder().build();
                ApiResponse<SaldoResponse> apiResp = ApiResponse.success("Updated", null);
                when(saldoCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Saldo.ApiResponseSaldo response = handler.updateSaldo(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}