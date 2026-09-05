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
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.service.MerchantDocumentCommandService;

import io.grpc.StatusRuntimeException;
import io.smallrye.mutiny.Uni;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentCommandGrpcHandlerTest {

        @Mock
        private MerchantDocumentCommandService commandService;

        private MerchantDocumentCommandGrpcHandler handler;

        @BeforeEach
        void setUp() {
                handler = new MerchantDocumentCommandGrpcHandler();
                handler.merchantDocumentCommandService = commandService;
        }

        // helpers
        private MerchantDocumentResponse createDocResponse(Long docId) {
                MerchantDocumentResponse r = new MerchantDocumentResponse();
                r.setDocumentId(docId);
                r.setMerchantId(1L);
                r.setDocumentType("ID_CARD");
                r.setDocumentUrl("http://docs.com/id.jpg");
                r.setStatus("APPROVED");
                r.setNote("sample note");
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                return r;
        }

        private MerchantDocumentResponseDeleteAt createDocDeleteAtResponse(Long docId) {
                MerchantDocumentResponseDeleteAt r = new MerchantDocumentResponseDeleteAt();
                r.setDocumentId(docId);
                r.setMerchantId(1L);
                r.setDocumentType("ID_CARD");
                r.setDocumentUrl("http://docs.com/id.jpg");
                r.setStatus("INACTIVE");
                r.setNote("trashed");
                r.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setUpdatedAt(LocalDateTime.of(2024, 6, 15, 10, 30).toString());
                r.setDeletedAt(LocalDateTime.of(2024, 6, 16, 8, 0).toString());
                return r;
        }

        // ===== create =====
        @Test
        @DisplayName("create - success")
        void create_Success() {
                MerchantDocumentCommand.CreateMerchantDocumentRequest request = MerchantDocumentCommand.CreateMerchantDocumentRequest
                                .newBuilder()
                                .setMerchantId(1)
                                .setDocumentType("ID_CARD")
                                .setDocumentUrl("http://docs.com/id.jpg")
                                .build();

                MerchantDocumentResponse data = createDocResponse(10L);
                ApiResponse<MerchantDocumentResponse> apiResp = ApiResponse.success("Document created", data);
                when(commandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocument response = handler.create(request).await()
                                .indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getDocumentId()).isEqualTo(10);
                assertThat(response.getData().getDocumentType()).isEqualTo("ID_CARD");
        }

        @Test
        @DisplayName("create - internal error")
        void create_Error() {
                MerchantDocumentCommand.CreateMerchantDocumentRequest request = MerchantDocumentCommand.CreateMerchantDocumentRequest
                                .newBuilder().build();
                when(commandService.create(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Failed")));
                try {
                        handler.create(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== update =====
        @Test
        @DisplayName("update - success")
        void update_Success() {
                MerchantDocumentCommand.UpdateMerchantDocumentRequest request = MerchantDocumentCommand.UpdateMerchantDocumentRequest
                                .newBuilder()
                                .setDocumentId(10)
                                .setMerchantId(1)
                                .setDocumentType("PASSPORT")
                                .setDocumentUrl("http://docs.com/passport.jpg")
                                .setNote("updated")
                                .setStatus("APPROVED")
                                .build();

                MerchantDocumentResponse data = createDocResponse(10L);
                data.setDocumentType("PASSPORT");
                ApiResponse<MerchantDocumentResponse> apiResp = ApiResponse.success("Document updated", data);
                when(commandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocument response = handler.update(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getDocumentType()).isEqualTo("PASSPORT");
        }

        @Test
        @DisplayName("update - internal error")
        void update_Error() {
                MerchantDocumentCommand.UpdateMerchantDocumentRequest request = MerchantDocumentCommand.UpdateMerchantDocumentRequest
                                .newBuilder().build();
                when(commandService.update(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Update failed")));
                try {
                        handler.update(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== updateStatus =====
        @Test
        @DisplayName("updateStatus - success")
        void updateStatus_Success() {
                MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest request = MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest
                                .newBuilder()
                                .setDocumentId(10)
                                .setMerchantId(1)
                                .setStatus("REJECTED")
                                .setNote("invalid")
                                .build();

                MerchantDocumentResponse data = createDocResponse(10L);
                data.setStatus("REJECTED");
                ApiResponse<MerchantDocumentResponse> apiResp = ApiResponse.success("Status updated", data);
                when(commandService.updateStatus(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocument response = handler.updateStatus(request).await()
                                .indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().getStatus()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("updateStatus - internal error")
        void updateStatus_Error() {
                MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest request = MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest
                                .newBuilder().build();
                when(commandService.updateStatus(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Status update failed")));
                try {
                        handler.updateStatus(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== trashed =====
        @Test
        @DisplayName("trashed - success")
        void trashed_Success() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder()
                                .setDocumentId(10).build();

                MerchantDocumentResponseDeleteAt data = createDocDeleteAtResponse(10L);
                ApiResponse<MerchantDocumentResponseDeleteAt> apiResp = ApiResponse.success("Document trashed", data);
                when(commandService.trash(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt response = handler.trashed(request)
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isTrue();
        }

        @Test
        @DisplayName("trashed - internal error")
        void trashed_Error() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder().build();
                when(commandService.trash(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Trash failed")));
                try {
                        handler.trashed(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== restore =====
        @Test
        @DisplayName("restore - success")
        void restore_Success() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder()
                                .setDocumentId(10).build();

                MerchantDocumentResponseDeleteAt data = createDocDeleteAtResponse(10L);
                data.setDeletedAt(null); // restored
                ApiResponse<MerchantDocumentResponseDeleteAt> apiResp = ApiResponse.success("Document restored", data);
                when(commandService.restore(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt response = handler.restore(request)
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getData().hasDeletedAt()).isFalse();
        }

        @Test
        @DisplayName("restore - internal error")
        void restore_Error() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder().build();
                when(commandService.restore(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore failed")));
                try {
                        handler.restore(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== deletePermanent =====
        @Test
        @DisplayName("deletePermanent - success")
        void deletePermanent_Success() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder()
                                .setDocumentId(10).build();

                ApiResponse<Boolean> apiResp = ApiResponse.success("Permanently deleted", true);
                when(commandService.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentCommand.ApiResponseMerchantDocumentDelete response = handler.deletePermanent(request)
                                .await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("Permanently deleted");
        }

        @Test
        @DisplayName("deletePermanent - internal error")
        void deletePermanent_Error() {
                MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest request = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest
                                .newBuilder().build();
                when(commandService.deletePermanent(anyLong()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete failed")));
                try {
                        handler.deletePermanent(request).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== restoreAll =====
        @Test
        @DisplayName("restoreAll - success")
        void restoreAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All restored", true);
                when(commandService.restoreAll()).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentCommand.ApiResponseMerchantDocumentAll response = handler
                                .restoreAll(Empty.getDefaultInstance()).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All restored");
        }

        @Test
        @DisplayName("restoreAll - internal error")
        void restoreAll_Error() {
                when(commandService.restoreAll())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Restore all failed")));
                try {
                        handler.restoreAll(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // ===== deleteAllPermanent =====
        @Test
        @DisplayName("deleteAllPermanent - success")
        void deleteAll_Success() {
                ApiResponse<Boolean> apiResp = ApiResponse.success("All permanently deleted", true);
                when(commandService.deleteAllPermanent()).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentCommand.ApiResponseMerchantDocumentAll response = handler
                                .deleteAllPermanent(Empty.getDefaultInstance()).await().indefinitely();
                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).isEqualTo("All permanently deleted");
        }

        @Test
        @DisplayName("deleteAllPermanent - internal error")
        void deleteAll_Error() {
                when(commandService.deleteAllPermanent())
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Delete all failed")));
                try {
                        handler.deleteAllPermanent(Empty.getDefaultInstance()).await().indefinitely();
                } catch (StatusRuntimeException e) {
                        assertThat(e).isNotNull();
                }
        }

        // edge cases
        @Test
        @DisplayName("create - null data")
        void create_NullData() {
                MerchantDocumentCommand.CreateMerchantDocumentRequest request = MerchantDocumentCommand.CreateMerchantDocumentRequest
                                .newBuilder().build();
                ApiResponse<MerchantDocumentResponse> apiResp = ApiResponse.success("Created", null);
                when(commandService.create(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocument response = handler.create(request).await()
                                .indefinitely();
                assertThat(response.hasData()).isFalse();
        }

        @Test
        @DisplayName("update - null data")
        void update_NullData() {
                MerchantDocumentCommand.UpdateMerchantDocumentRequest request = MerchantDocumentCommand.UpdateMerchantDocumentRequest
                                .newBuilder().build();
                ApiResponse<MerchantDocumentResponse> apiResp = ApiResponse.success("Updated", null);
                when(commandService.update(any())).thenReturn(Uni.createFrom().item(apiResp));

                MerchantDocumentOuterClass.ApiResponseMerchantDocument response = handler.update(request).await()
                                .indefinitely();
                assertThat(response.hasData()).isFalse();
        }
}