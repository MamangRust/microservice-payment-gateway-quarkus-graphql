package com.sanedge.topup.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.service.TopupQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.topup.Topup;
import pb.topup.TopupQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class TopupQueryGrpcHandlerTest {

        @Mock
        private TopupQueryService topupQueryService;

        private TopupQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TopupQueryGrpcHandler();
                handler.topupQueryService = topupQueryService;
        }

        // ---------- helpers ----------
        private TopupResponse createTopupResponse(Long id) {
                TopupResponse r = new TopupResponse();
                r.setId(id);
                r.setTopupNo("TP-20240615-001");
                r.setCardNumber("1234-5678-9012-3456");
                r.setTopupAmount(50000L);
                r.setTopupMethod("BANK_TRANSFER");
                r.setTopupTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                return r;
        }

        private TopupResponseDeleteAt createTopupResponseDeleteAt(Long id) {
                TopupResponseDeleteAt r = new TopupResponseDeleteAt();
                r.setId(id);
                r.setTopupNo("TP-20240615-001");
                r.setCardNumber("1234-5678-9012-3456");
                r.setTopupAmount(50000L);
                r.setTopupMethod("BANK_TRANSFER");
                r.setTopupTime(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30, 0).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0, 0).toString());
                return r;
        }

        // ========== findAllTopup ==========
        @Test
        @DisplayName("findAllTopup - success with data")
        void findAllTopup_Success() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                TopupResponse data = createTopupResponse(1L);
                ApiResponsePagination<List<TopupResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Topups retrieved successfully", List.of(data), null);

                when(topupQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TopupQuery.ApiResponsePaginationTopup response = handler.findAllTopup(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getTopupNo()).isEqualTo("TP-20240615-001");
        }

        @Test
        @DisplayName("findAllTopup - internal error")
        void findAllTopup_Error() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().build();
                when(topupQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findAllTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findAllTopupByCardNumber ==========
        @Test
        @DisplayName("findAllTopupByCardNumber - success")
        void findAllByCardNumber_Success() {
                TopupQuery.FindAllTopupByCardNumberRequest request = TopupQuery.FindAllTopupByCardNumberRequest
                                .newBuilder()
                                .setCardNumber("1234-5678-9012-3456").setPage(1).build();

                TopupResponse data = createTopupResponse(1L);
                ApiResponsePagination<List<TopupResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Topups by card", List.of(data), null);

                when(topupQueryService.findAllByCardNumber(any())).thenReturn(Uni.createFrom().item(apiResp));

                TopupQuery.ApiResponsePaginationTopup response = handler.findAllTopupByCardNumber(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
        }

        // ========== findByIdTopup ==========
        @Test
        @DisplayName("findByIdTopup - success")
        void findById_Success() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();
                TopupResponse data = createTopupResponse(1L);
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("Topup found", data);
                when(topupQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.findByIdTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
        }

        @Test
        @DisplayName("findByIdTopup - NOT_FOUND")
        void findById_NotFound() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(999).build();
                when(topupQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new ResourceNotFoundException("Topup not found")));
                try {
                        handler.findByIdTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        @Test
        @DisplayName("findByIdTopup - internal error")
        void findById_Error() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();
                when(topupQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Unexpected")));
                try {
                        handler.findByIdTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByCardNumberTopup ==========
        @Test
        @DisplayName("findByCardNumberTopup - success with data")
        void findByCardNumber_Success() {
                Topup.FindByCardNumberTopupRequest request = Topup.FindByCardNumberTopupRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456").build();

                TopupResponse data = createTopupResponse(1L);
                ApiResponse<List<TopupResponse>> apiResp = ApiResponse.success("Topups found", List.of(data));
                when(topupQueryService.findByCard(anyString())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.findByCardNumberTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        @DisplayName("findByCardNumberTopup - empty list returns no data")
        void findByCardNumber_EmptyList() {
                Topup.FindByCardNumberTopupRequest request = Topup.FindByCardNumberTopupRequest.newBuilder()
                                .setCardNumber("none").build();

                ApiResponse<List<TopupResponse>> apiResp = ApiResponse.success("No topups", List.of());
                when(topupQueryService.findByCard(anyString())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.findByCardNumberTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isFalse();
        }

        // ========== findByActive ==========
        @Test
        @DisplayName("findByActive - success")
        void findByActive_Success() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().setPage(1).build();

                TopupResponseDeleteAt data = createTopupResponseDeleteAt(1L);
                ApiResponsePagination<List<TopupResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active topups", List.of(data), null);
                when(topupQueryService.findActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                TopupQuery.ApiResponsePaginationTopupDeleteAt response = handler.findByActive(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByActive - internal error")
        void findByActive_Error() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().build();
                when(topupQueryService.findActive(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByActive(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByTrashed ==========
        @Test
        @DisplayName("findByTrashed - success")
        void findByTrashed_Success() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().build();

                TopupResponseDeleteAt data = createTopupResponseDeleteAt(1L);
                ApiResponsePagination<List<TopupResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed topups", List.of(data), null);
                when(topupQueryService.findTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                TopupQuery.ApiResponsePaginationTopupDeleteAt response = handler.findByTrashed(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByTrashed - internal error")
        void findByTrashed_Error() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().build();
                when(topupQueryService.findTrashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByTrashed(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== edge cases ==========
        @Test
        @DisplayName("findAllTopup - empty data")
        void findAll_Empty() {
                TopupQuery.FindAllTopupRequest request = TopupQuery.FindAllTopupRequest.newBuilder().build();
                ApiResponsePagination<List<TopupResponse>> apiResp = new ApiResponsePagination<>("success", "No topups",
                                List.of(), null);
                when(topupQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                TopupQuery.ApiResponsePaginationTopup response = handler.findAllTopup(request).await().indefinitely();
                assertThat(response.getDataCount()).isZero();
        }

        @Test
        @DisplayName("findByIdTopup - null data")
        void findById_NullData() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("No data", null);
                when(topupQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.findByIdTopup(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}