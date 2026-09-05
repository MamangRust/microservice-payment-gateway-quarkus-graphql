package com.sanedge.withdraw.handler;

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
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.service.WithdrawCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.withdraw.Withdraw;
import pb.withdraw.WithdrawCommand;

@ExtendWith(MockitoExtension.class)
class WithdrawCommandGrpcHandlerTest {

        @Mock
        private WithdrawCommandService withdrawCommandService;

        private WithdrawCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new WithdrawCommandGrpcHandler();
                handler.withdrawCommandService = withdrawCommandService;
        }

        private WithdrawResponse createWithdrawResponse(Long id) {
                WithdrawResponse response = new WithdrawResponse();
                response.setId(id);
                response.setCardNumber("1234567890123456");
                response.setWithdrawAmount(150000L);
                response.setWithdrawTime((LocalDateTime.of(2024, 6, 15, 10, 30, 0)).toString());
                response.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                response.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                return response;
        }

        @Test
        @DisplayName("createWithdraw - should return ApiResponseWithdraw on success")
        void createWithdraw_Success() {
                WithdrawCommand.CreateWithdrawRequest request = WithdrawCommand.CreateWithdrawRequest.newBuilder()
                                .setCardNumber("1234567890123456").setWithdrawAmount(150000).build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponse<WithdrawResponse> apiResp = ApiResponse.success("Withdraw created successfully", data);

                when(withdrawCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApiResponseWithdraw response = handler.createWithdraw(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getWithdrawId()).isEqualTo(1);
        }

        @Test
        @DisplayName("createWithdraw - should return INTERNAL on failure")
        void createWithdraw_InternalError() {
                WithdrawCommand.CreateWithdrawRequest request = WithdrawCommand.CreateWithdrawRequest.newBuilder()
                                .setCardNumber("1234567890123456").setWithdrawAmount(150000).build();

                when(withdrawCommandService.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.createWithdraw(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("updateWithdraw - should return ApiResponseWithdraw on success")
        void updateWithdraw_Success() {
                WithdrawCommand.UpdateWithdrawRequest request = WithdrawCommand.UpdateWithdrawRequest.newBuilder()
                                .setWithdrawId(1).setCardNumber("1234567890123456").setWithdrawAmount(200000).build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponse<WithdrawResponse> apiResp = ApiResponse.success("Withdraw updated successfully", data);

                when(withdrawCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApiResponseWithdraw response = handler.updateWithdraw(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("trashedWithdraw - should return ApIResponseWithdrawDeleteAt on success")
        void trashedWithdraw_Success() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(1).build();

                WithdrawResponseDeleteAt data = new WithdrawResponseDeleteAt();
                data.setId(1L);
                ApiResponse<WithdrawResponseDeleteAt> apiResp = ApiResponse.success("Withdraw trashed successfully",
                                data);

                when(withdrawCommandService.trashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApIResponseWithdrawDeleteAt response = handler.trashedWithdraw(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getWithdrawId()).isEqualTo(1);
        }

        @Test
        @DisplayName("restoreWithdraw - should return ApIResponseWithdrawDeleteAt on success")
        void restoreWithdraw_Success() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(1).build();

                WithdrawResponseDeleteAt data = new WithdrawResponseDeleteAt();
                data.setId(1L);
                ApiResponse<WithdrawResponseDeleteAt> apiResp = ApiResponse.success("Withdraw restored successfully",
                                data);

                when(withdrawCommandService.restore(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApIResponseWithdrawDeleteAt response = handler.restoreWithdraw(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteWithdrawPermanent - should return ApiResponseWithdrawDelete on success")
        void deleteWithdrawPermanent_Success() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(1).build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Withdraw permanently deleted", true);

                when(withdrawCommandService.deletePermanent(any())).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawCommand.ApiResponseWithdrawDelete response = handler.deleteWithdrawPermanent(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Withdraw permanently deleted");
        }

        @Test
        @DisplayName("restoreAllWithdraw - should return ApiResponseWithdrawAll on success")
        void restoreAllWithdraw_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All trashed withdraws restored successfully",
                                true);

                when(withdrawCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawCommand.ApiResponseWithdrawAll response = handler.restoreAllWithdraw(Empty.getDefaultInstance())
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("deleteAllWithdrawPermanent - should return ApiResponseWithdrawAll on success")
        void deleteAllWithdrawPermanent_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All trashed withdraws permanently deleted",
                                true);

                when(withdrawCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawCommand.ApiResponseWithdrawAll response = handler
                                .deleteAllWithdrawPermanent(Empty.getDefaultInstance()).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
        }
}