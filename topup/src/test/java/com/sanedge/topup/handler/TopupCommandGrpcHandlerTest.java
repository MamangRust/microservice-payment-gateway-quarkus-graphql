package com.sanedge.topup.handler;

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
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.service.TopupCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.topup.Topup;
import pb.topup.TopupCommand;

@ExtendWith(MockitoExtension.class)
class TopupCommandGrpcHandlerTest {

        @Mock
        private TopupCommandService topupCommandService;

        private TopupCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new TopupCommandGrpcHandler();
                handler.topupCommandService = topupCommandService;
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

        // ========== createTopup ==========
        @Test
        @DisplayName("createTopup - success")
        void create_Success() {
                TopupCommand.CreateTopupRequest request = TopupCommand.CreateTopupRequest.newBuilder()
                                .setCardNumber("1234-5678-9012-3456")
                                .setTopupAmount(50000)
                                .setTopupMethod("BANK_TRANSFER")
                                .build();

                TopupResponse data = createTopupResponse(1L);
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("Topup created successfully", data);
                when(topupCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.createTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Topup created successfully");
                assertThat(response.getData().getId()).isEqualTo(1);
                assertThat(response.getData().getTopupAmount()).isEqualTo(50000);
        }

        @Test
        @DisplayName("createTopup - internal error")
        void create_Error() {
                TopupCommand.CreateTopupRequest request = TopupCommand.CreateTopupRequest.newBuilder().build();
                when(topupCommandService.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Failed")));
                try {
                        handler.createTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== updateTopup ==========
        @Test
        @DisplayName("updateTopup - success")
        void update_Success() {
                TopupCommand.UpdateTopupRequest request = TopupCommand.UpdateTopupRequest.newBuilder()
                                .setTopupId(1)
                                .setCardNumber("1234-5678-9012-3456")
                                .setTopupAmount(75000)
                                .setTopupMethod("BANK_TRANSFER")
                                .build();

                TopupResponse data = createTopupResponse(1L);
                data.setTopupAmount(75000L);
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("Topup updated successfully", data);
                when(topupCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.updateTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getTopupAmount()).isEqualTo(75000);
        }

        @Test
        @DisplayName("updateTopup - internal error")
        void update_Error() {
                TopupCommand.UpdateTopupRequest request = TopupCommand.UpdateTopupRequest.newBuilder().build();
                when(topupCommandService.update(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));
                try {
                        handler.updateTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== trashedTopup ==========
        @Test
        @DisplayName("trashedTopup - success")
        void trash_Success() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();

                TopupResponseDeleteAt data = createTopupResponseDeleteAt(1L);
                ApiResponse<TopupResponseDeleteAt> apiResp = ApiResponse.success("Topup trashed", data);
                when(topupCommandService.trashed(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopupDeleteAt response = handler.trashedTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashedTopup - internal error")
        void trash_Error() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().build();
                when(topupCommandService.trashed(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));
                try {
                        handler.trashedTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreTopup ==========
        @Test
        @DisplayName("restoreTopup - success")
        void restore_Success() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();

                TopupResponseDeleteAt data = createTopupResponseDeleteAt(1L);
                ApiResponse<TopupResponseDeleteAt> apiResp = ApiResponse.success("Topup restored", data);
                when(topupCommandService.restore(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopupDeleteAt response = handler.restoreTopup(request).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
        }

        @Test
        @DisplayName("restoreTopup - internal error")
        void restore_Error() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().build();
                when(topupCommandService.restore(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));
                try {
                        handler.restoreTopup(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteTopupPermanent ==========
        @Test
        @DisplayName("deleteTopupPermanent - success")
        void deletePermanent_Success() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().setTopupId(1).build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Topup permanently deleted!", true);
                when(topupCommandService.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                TopupCommand.ApiResponseTopupDelete response = handler.deleteTopupPermanent(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Topup permanently deleted!");
        }

        @Test
        @DisplayName("deleteTopupPermanent - internal error")
        void deletePermanent_Error() {
                Topup.FindByIdTopupRequest request = Topup.FindByIdTopupRequest.newBuilder().build();
                when(topupCommandService.deletePermanent(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));
                try {
                        handler.deleteTopupPermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== restoreAllTopup ==========
        @Test
        @DisplayName("restoreAllTopup - success")
        void restoreAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All topups restored successfully!", true);
                when(topupCommandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                TopupCommand.ApiResponseTopupAll response = handler.restoreAllTopup(Empty.getDefaultInstance()).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All topups restored successfully!");
        }

        @Test
        @DisplayName("restoreAllTopup - internal error")
        void restoreAll_Error() {
                when(topupCommandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));
                try {
                        handler.restoreAllTopup(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== deleteAllTopupPermanent ==========
        @Test
        @DisplayName("deleteAllTopupPermanent - success")
        void deleteAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All topups permanently deleted!", true);
                when(topupCommandService.deleteAll()).thenReturn(Uni.createFrom().item(apiResp));

                TopupCommand.ApiResponseTopupAll response = handler.deleteAllTopupPermanent(Empty.getDefaultInstance())
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All topups permanently deleted!");
        }

        @Test
        @DisplayName("deleteAllTopupPermanent - internal error")
        void deleteAll_Error() {
                when(topupCommandService.deleteAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));
                try {
                        handler.deleteAllTopupPermanent(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ========== edge cases ==========
        @Test
        @DisplayName("createTopup - null data")
        void create_NullData() {
                TopupCommand.CreateTopupRequest request = TopupCommand.CreateTopupRequest.newBuilder().build();
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("Created", null);
                when(topupCommandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.createTopup(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("updateTopup - null data")
        void update_NullData() {
                TopupCommand.UpdateTopupRequest request = TopupCommand.UpdateTopupRequest.newBuilder().build();
                ApiResponse<TopupResponse> apiResp = ApiResponse.success("Updated", null);
                when(topupCommandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                Topup.ApiResponseTopup response = handler.updateTopup(request).await().indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}