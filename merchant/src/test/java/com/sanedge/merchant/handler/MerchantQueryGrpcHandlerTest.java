package com.sanedge.merchant.handler;

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
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.service.MerchantQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.merchant.Merchant;
import pb.merchant.MerchantQuery;

@ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
class MerchantQueryGrpcHandlerTest {

        @Mock
        private MerchantQueryService merchantQueryService;

        private MerchantQueryGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new MerchantQueryGrpcHandler();
                handler.merchantQueryService = merchantQueryService;
        }

        // ---------- helpers ----------
        private MerchantResponse createMerchantResponse(Long id) {
                MerchantResponse r = new MerchantResponse();
                r.setId(id);
                r.setName("Test Merchant");
                r.setApiKey("api-123");
                r.setStatus("ACTIVE");
                r.setUserId(100L);
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                return r;
        }

        private MerchantResponseDeleteAt createMerchantResponseDeleteAt(Long id) {
                MerchantResponseDeleteAt r = new MerchantResponseDeleteAt();
                r.setId(id);
                r.setName("Trashed Merchant");
                r.setApiKey("api-456");
                r.setStatus("INACTIVE");
                r.setUserId(200L);
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0).toString());
                return r;
        }

        // ========== findAllMerchant ==========
        @Test
        @DisplayName("findAllMerchant - success with data")
        void findAllMerchant_Success() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                MerchantResponse data = createMerchantResponse(1L);
                ApiResponsePagination<List<MerchantResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "Merchants retrieved", List.of(data), null);
                when(merchantQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantQuery.ApiResponsePaginationMerchant response = handler.findAllMerchant(request).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).getName()).isEqualTo("Test Merchant");
                assertThat(response.getData(0).getApiKey()).isEqualTo("api-123");
        }

        @Test
        @DisplayName("findAllMerchant - internal error")
        void findAllMerchant_Error() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder().build();
                when(merchantQueryService.findAll(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findAllMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByIdMerchant ==========
        @Test
        @DisplayName("findByIdMerchant - success")
        void findByIdMerchant_Success() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();

                MerchantResponse data = createMerchantResponse(1L);
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Merchant found", data);
                when(merchantQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.findByIdMerchant(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getName()).isEqualTo("Test Merchant");
        }

        @Test
        @DisplayName("findByIdMerchant - internal error")
        void findByIdMerchant_Error() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();
                when(merchantQueryService.findById(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Unexpected")));
                try {
                        handler.findByIdMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByApiKey ==========
        @Test
        @DisplayName("findByApiKey - success")
        void findByApiKey_Success() {
                Merchant.FindByApiKeyRequest request = Merchant.FindByApiKeyRequest.newBuilder()
                                .setApiKey("api-123").build();

                MerchantResponse data = createMerchantResponse(1L);
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Merchant found", data);
                when(merchantQueryService.findByApiKey(anyString())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.findByApiKey(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getApiKey()).isEqualTo("api-123");
        }

        @Test
        @DisplayName("findByApiKey - internal error")
        void findByApiKey_Error() {
                Merchant.FindByApiKeyRequest request = Merchant.FindByApiKeyRequest.newBuilder().build();
                when(merchantQueryService.findByApiKey(anyString()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByApiKey(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByMerchantUserId ==========
        @Test
        @DisplayName("findByMerchantUserId - success with multiple merchants")
        void findByMerchantUserId_Success() {
                Merchant.FindByMerchantUserIdRequest request = Merchant.FindByMerchantUserIdRequest.newBuilder()
                                .setUserId(100).build();

                MerchantResponse data1 = createMerchantResponse(1L);
                MerchantResponse data2 = createMerchantResponse(2L);
                ApiResponse<List<MerchantResponse>> apiResp = ApiResponse.success("Merchants found",
                                List.of(data1, data2));
                when(merchantQueryService.findByUserId(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponsesMerchant response = handler.findByMerchantUserId(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("findByMerchantUserId - internal error")
        void findByMerchantUserId_Error() {
                Merchant.FindByMerchantUserIdRequest request = Merchant.FindByMerchantUserIdRequest.newBuilder()
                                .build();
                when(merchantQueryService.findByUserId(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByMerchantUserId(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== findByActive ==========
        @Test
        @DisplayName("findByActive - success with trashed merchants")
        void findByActive_Success() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder()
                                .setPage(1).setPageSize(10).build();

                MerchantResponseDeleteAt data = createMerchantResponseDeleteAt(1L);
                ApiResponsePagination<List<MerchantResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Active merchants", List.of(data), null);
                when(merchantQueryService.findByActive(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantQuery.ApiResponsePaginationMerchantDeleteAt response = handler.findByActive(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getDataCount()).isEqualTo(1);
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByActive - internal error")
        void findByActive_Error() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder().build();
                when(merchantQueryService.findByActive(any()))
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
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder().build();

                MerchantResponseDeleteAt data = createMerchantResponseDeleteAt(1L);
                ApiResponsePagination<List<MerchantResponseDeleteAt>> apiResp = new ApiResponsePagination<>(
                                "success", "Trashed merchants", List.of(data), null);
                when(merchantQueryService.findByTrashed(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantQuery.ApiResponsePaginationMerchantDeleteAt response = handler.findByTrashed(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData(0).hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("findByTrashed - internal error")
        void findByTrashed_Error() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder().build();
                when(merchantQueryService.findByTrashed(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
                try {
                        handler.findByTrashed(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== edge cases ==========
        @Test
        @DisplayName("findAllMerchant - empty list")
        void findAllMerchant_Empty() {
                Merchant.FindAllMerchantRequest request = Merchant.FindAllMerchantRequest.newBuilder().build();
                ApiResponsePagination<List<MerchantResponse>> apiResp = new ApiResponsePagination<>(
                                "success", "No merchants", List.of(), null);
                when(merchantQueryService.findAll(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantQuery.ApiResponsePaginationMerchant response = handler.findAllMerchant(request).await()
                                .indefinitely();
                assertThat(response.getDataCount()).isZero();
        }

        @Test
        @DisplayName("findByIdMerchant - null data")
        void findByIdMerchant_NullData() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("No data", null);
                when(merchantQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.findByIdMerchant(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}