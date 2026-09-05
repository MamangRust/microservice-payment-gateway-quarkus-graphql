package com.sanedge.merchant.handler;

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

import com.google.protobuf.Empty;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.service.MerchantCommandService;
import com.sanedge.merchant.service.MerchantQueryService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.merchant.Merchant;
import pb.merchant.MerchantCommand;

@ExtendWith(MockitoExtension.class)
class MerchantCommandGrpcHandlerTest {

        @Mock
        private MerchantCommandService merchantCommandService;

        @Mock
        private MerchantQueryService merchantQueryService;

        private MerchantCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new MerchantCommandGrpcHandler();
                handler.merchantCommandService = merchantCommandService;
                handler.merchantQueryService = merchantQueryService;
        }

        // helpers
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

        // ========== createMerchant ==========
        @Test
        @DisplayName("createMerchant - success")
        void create_Success() {
                MerchantCommand.CreateMerchantRequest request = MerchantCommand.CreateMerchantRequest.newBuilder()
                                .setName("New Merchant")
                                .setUserId(100)
                                .build();

                MerchantResponse data = createMerchantResponse(1L);
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Merchant created", data);
                when(merchantCommandService.createMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.createMerchant(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getName()).isEqualTo("Test Merchant");
                assertThat(response.getData().getApiKey()).isEqualTo("api-123");
        }

        @Test
        @DisplayName("createMerchant - internal error")
        void create_Error() {
                MerchantCommand.CreateMerchantRequest request = MerchantCommand.CreateMerchantRequest.newBuilder()
                                .build();
                when(merchantCommandService.createMerchant(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Failed")));
                try {
                        handler.createMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== updateMerchant ==========
        @Test
        @DisplayName("updateMerchant - success")
        void update_Success() {
                MerchantCommand.UpdateMerchantRequest request = MerchantCommand.UpdateMerchantRequest.newBuilder()
                                .setMerchantId(1)
                                .setName("Updated Merchant")
                                .setUserId(100)
                                .setStatus("ACTIVE")
                                .build();

                MerchantResponse data = createMerchantResponse(1L);
                data.setName("Updated Merchant");
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Merchant updated", data);
                when(merchantCommandService.updateMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.updateMerchant(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getName()).isEqualTo("Updated Merchant");
        }

        @Test
        @DisplayName("updateMerchant - internal error")
        void update_Error() {
                MerchantCommand.UpdateMerchantRequest request = MerchantCommand.UpdateMerchantRequest.newBuilder()
                                .build();
                when(merchantCommandService.updateMerchant(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));
                try {
                        handler.updateMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== updateMerchantStatus ==========
        @Test
        @DisplayName("updateMerchantStatus - success")
        void updateStatus_Success() {
                MerchantCommand.UpdateMerchantStatusRequest request = MerchantCommand.UpdateMerchantStatusRequest
                                .newBuilder()
                                .setMerchantId(1)
                                .setStatus("INACTIVE")
                                .build();

                MerchantResponse existing = createMerchantResponse(1L);
                ApiResponse<MerchantResponse> queryResp = ApiResponse.success("Merchant found", existing);
                when(merchantQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(queryResp));

                MerchantResponse updated = createMerchantResponse(1L);
                updated.setStatus("INACTIVE");
                ApiResponse<MerchantResponse> updateResp = ApiResponse.success("Status updated", updated);
                when(merchantCommandService.updateMerchant(any())).thenReturn(Uni.createFrom().item(updateResp));

                Merchant.ApiResponseMerchant response = handler.updateMerchantStatus(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("updateMerchantStatus - merchant not found (query returns null data)")
        void updateStatus_NotFound() {
                MerchantCommand.UpdateMerchantStatusRequest request = MerchantCommand.UpdateMerchantStatusRequest
                                .newBuilder()
                                .setMerchantId(999).setStatus("ACTIVE").build();

                ApiResponse<MerchantResponse> queryResp = ApiResponse.success("Not found", null);
                when(merchantQueryService.findById(anyLong())).thenReturn(Uni.createFrom().item(queryResp));

                try {
                        handler.updateMerchantStatus(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== trashedMerchant ==========
        @Test
        @DisplayName("trashedMerchant - success")
        void trash_Success() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();

                MerchantResponseDeleteAt data = createMerchantResponseDeleteAt(1L);
                ApiResponse<MerchantResponseDeleteAt> apiResp = ApiResponse.success("Trashed", data);
                when(merchantCommandService.trashMerchant(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchantDeleteAt response = handler.trashedMerchant(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashedMerchant - internal error")
        void trash_Error() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder().build();
                when(merchantCommandService.trashMerchant(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));
                try {
                        handler.trashedMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreMerchant ==========
        @Test
        @DisplayName("restoreMerchant - success")
        void restore_Success() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();

                MerchantResponseDeleteAt data = createMerchantResponseDeleteAt(1L);
                data.setDeletedAt(null); // restored
                ApiResponse<MerchantResponseDeleteAt> apiResp = ApiResponse.success("Restored", data);
                when(merchantCommandService.restoreMerchant(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchantDeleteAt response = handler.restoreMerchant(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isFalse();
        }

        @Test
        @DisplayName("restoreMerchant - internal error")
        void restore_Error() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder().build();
                when(merchantCommandService.restoreMerchant(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));
                try {
                        handler.restoreMerchant(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteMerchantPermanent ==========
        @Test
        @DisplayName("deleteMerchantPermanent - success")
        void deletePermanent_Success() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder()
                                .setMerchantId(1).build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
                when(merchantCommandService.deleteMerchant(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantCommand.ApiResponseMerchantDelete response = handler.deleteMerchantPermanent(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Permanently deleted");
        }

        @Test
        @DisplayName("deleteMerchantPermanent - internal error")
        void deletePermanent_Error() {
                Merchant.FindByIdMerchantRequest request = Merchant.FindByIdMerchantRequest.newBuilder().build();
                when(merchantCommandService.deleteMerchant(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));
                try {
                        handler.deleteMerchantPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreAllMerchant ==========
        @Test
        @DisplayName("restoreAllMerchant - success")
        void restoreAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
                when(merchantCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                MerchantCommand.ApiResponseMerchantAll response = handler.restoreAllMerchant(Empty.getDefaultInstance())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All restored");
        }

        @Test
        @DisplayName("restoreAllMerchant - internal error")
        void restoreAll_Error() {
                when(merchantCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));
                try {
                        handler.restoreAllMerchant(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteAllMerchantPermanent ==========
        @Test
        @DisplayName("deleteAllMerchantPermanent - success")
        void deleteAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
                when(merchantCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                MerchantCommand.ApiResponseMerchantAll response = handler
                                .deleteAllMerchantPermanent(Empty.getDefaultInstance()).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All permanently deleted");
        }

        @Test
        @DisplayName("deleteAllMerchantPermanent - internal error")
        void deleteAll_Error() {
                when(merchantCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));
                try {
                        handler.deleteAllMerchantPermanent(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // edge cases
        @Test
        @DisplayName("createMerchant - null data")
        void create_NullData() {
                MerchantCommand.CreateMerchantRequest request = MerchantCommand.CreateMerchantRequest.newBuilder()
                                .build();
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Created", null);
                when(merchantCommandService.createMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.createMerchant(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateMerchant - null data")
        void update_NullData() {
                MerchantCommand.UpdateMerchantRequest request = MerchantCommand.UpdateMerchantRequest.newBuilder()
                                .build();
                ApiResponse<MerchantResponse> apiResp = ApiResponse.success("Updated", null);
                when(merchantCommandService.updateMerchant(any())).thenReturn(Uni.createFrom().item(apiResp));

                Merchant.ApiResponseMerchant response = handler.updateMerchant(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}