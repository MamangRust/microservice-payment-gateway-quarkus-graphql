package com.sanedge.withdraw.handler;

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
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.service.WithdrawQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.card.Card;
import pb.withdraw.Withdraw;
import pb.withdraw.WithdrawQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class WithdrawQueryGrpcHandlerTest {

        @Mock
        private WithdrawQueryService withdrawQueryService;

        private WithdrawQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new WithdrawQueryGrpcHandler();
                handler.withdrawQueryService = withdrawQueryService;
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
        @DisplayName("findAllWithdraw - should return pagination on success")
        void findAllWithdraw_Success() {
                Withdraw.FindAllWithdrawRequest request = Withdraw.FindAllWithdrawRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponsePagination<List<WithdrawResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Withdraws retrieved successfully", List.of(data), null);

                when(withdrawQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawQuery.ApiResponsePaginationWithdraw response = handler.findAllWithdraw(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findAllWithdraw - should return INTERNAL on failure")
        void findAllWithdraw_InternalError() {
                Withdraw.FindAllWithdrawRequest request = Withdraw.FindAllWithdrawRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                when(withdrawQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findAllWithdraw(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findAllWithdrawByCardNumber - should return pagination on success")
        void findAllWithdrawByCardNumber_Success() {
                Withdraw.FindAllWithdrawByCardNumberRequest request = Withdraw.FindAllWithdrawByCardNumberRequest
                                .newBuilder()
                                .setCardNumber("1234567890123456").setPage(1).setPageSize(10).build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponsePagination<List<WithdrawResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Withdraws retrieved successfully", List.of(data), null);

                when(withdrawQueryService.findAllByCardNumber(any())).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawQuery.ApiResponsePaginationWithdraw response = handler.findAllWithdrawByCardNumber(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByIdWithdraw - should return ApiResponseWithdraw on success")
        void findByIdWithdraw_Success() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(1).build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponse<WithdrawResponse> apiResp = ApiResponse.success("Withdraw retrieved successfully", data);

                when(withdrawQueryService.findById(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApiResponseWithdraw response = handler.findByIdWithdraw(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getWithdrawId()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByIdWithdraw - should return NOT_FOUND when withdraw not found")
        void findByIdWithdraw_NotFound() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(999).build();

                when(withdrawQueryService.findById(any()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new ResourceNotFoundException("Withdraw not found")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByIdWithdraw(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByIdWithdraw - should return INTERNAL on generic exception")
        void findByIdWithdraw_InternalError() {
                Withdraw.FindByIdWithdrawRequest request = Withdraw.FindByIdWithdrawRequest.newBuilder()
                                .setWithdrawId(1).build();

                when(withdrawQueryService.findById(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));

                StatusRuntimeException ex = null;
                try {
                        handler.findByIdWithdraw(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        ex = e;
                }

                assertThat(ex).isNotNull();
        }

        @Test
        @DisplayName("findByCardNumber - should return list on success")
        void findByCardNumber_Success() {
                Card.FindByCardNumberRequest request = Card.FindByCardNumberRequest.newBuilder()
                                .setCardNumber("1234567890123456").build();

                WithdrawResponse data = createWithdrawResponse(1L);
                ApiResponse<List<WithdrawResponse>> apiResp = ApiResponse.success("Withdraws retrieved successfully",
                                List.of(data));

                when(withdrawQueryService.findByCard(any())).thenReturn(Uni.createFrom().item(apiResp));

                Withdraw.ApiResponsesWithdraw response = handler.findByCardNumber(request).await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByActive - should return pagination on success")
        void findByActive_Success() {
                Withdraw.FindAllWithdrawRequest request = Withdraw.FindAllWithdrawRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                WithdrawResponseDeleteAt data = new WithdrawResponseDeleteAt();
                data.setId(1L);
                ApiResponsePagination<List<WithdrawResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active withdraws retrieved successfully", List.of(data), null);

                when(withdrawQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt response = handler.findByActive(request).await()
                                .indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByTrashed - should return pagination on success")
        void findByTrashed_Success() {
                Withdraw.FindAllWithdrawRequest request = Withdraw.FindAllWithdrawRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                WithdrawResponseDeleteAt data = new WithdrawResponseDeleteAt();
                data.setId(1L);
                ApiResponsePagination<List<WithdrawResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed withdraws retrieved successfully", List.of(data), null);

                when(withdrawQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt response = handler.findByTrashed(request)
                                .await().indefinitely();

                assertThat(response).isNotNull();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }
}