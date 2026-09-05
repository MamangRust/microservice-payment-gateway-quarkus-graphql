package com.sanedge.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.entity.Merchant;
import com.sanedge.merchant.entity.MerchantDocument;
import com.sanedge.merchant.repository.MerchantDocumentCommandRepository;
import com.sanedge.merchant.repository.MerchantDocumentQueryRepository;
import com.sanedge.merchant.repository.MerchantQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentCommandServiceImplTest {

        @Mock
        private MerchantQueryRepository merchantQueryRepo;
        @Mock
        private MerchantDocumentQueryRepository documentQueryRepo;
        @Mock
        private MerchantDocumentCommandRepository documentCommandRepo;
        @Mock
        private RedisService redisService;
    private TracingMetrics tracingMetrics;

        private MerchantDocumentCommandServiceImpl service;

        @BeforeEach
        void setUp() {                tracingMetrics = mock(TracingMetrics.class, withSettings().lenient());
                lenient().doAnswer(inv -> {
                        Supplier<Uni<?>> s = inv.getArgument(3);
                        return s.get();
                })
                                .when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
                lenient().doAnswer(inv -> {
                        Supplier<Uni<?>> s = inv.getArgument(2);
                        return s.get();
                })
                                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

                service = new MerchantDocumentCommandServiceImpl(
                        merchantQueryRepo, documentQueryRepo, documentCommandRepo, redisService,
                        tracingMetrics);

                lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());
                lenient().when(documentCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
                lenient().when(documentCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
        }

        private Merchant createMockMerchant(Long id) {
                Merchant m = new Merchant();
                m.setMerchantId(id);
                m.setName("Test Merchant");
                m.setApiKey("key");
                m.setUserId(100);
                return m;
        }

        private MerchantDocument createMockDocument(Long id) {
                MerchantDocument doc = new MerchantDocument();
                doc.setDocumentId(id);
                doc.setMerchantId(1);
                doc.setDocumentType("ID_CARD");
                doc.setDocumentUrl("http://docs.com/id.jpg");
                doc.setStatus("PENDING");
                doc.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                doc.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return doc;
        }

        private CreateMerchantDocumentRequest createReq() {
                CreateMerchantDocumentRequest req = new CreateMerchantDocumentRequest();
                req.setMerchantId(1L);
                req.setDocumentType("ID_CARD");
                req.setDocumentUrl("http://docs.com/id.jpg");
                return req;
        }

        private MerchantDocument createPersistedDocument(Long id) {
                MerchantDocument doc = new MerchantDocument();
                doc.setDocumentId(id);
                doc.setMerchantId(1);
                doc.setDocumentType("ID_CARD");
                doc.setDocumentUrl("http://docs.com/id.jpg");
                doc.setStatus("PENDING");
                doc.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                doc.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return doc;
        }

        // ---------- create ----------
        @Nested
        @DisplayName("create tests")
        class CreateTests {
                @Test
                void success() {
                        CreateMerchantDocumentRequest req = createReq();
                        when(merchantQueryRepo.findMerchantById(1L))
                                        .thenReturn(Uni.createFrom().item(createMockMerchant(1L)));                        when(documentCommandRepo.persist(any(MerchantDocument.class)))
                                        .thenReturn(Uni.createFrom().item(createPersistedDocument(10L)));

                        ApiResponse<MerchantDocumentResponse> resp = service.create(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data().getDocumentId()).isEqualTo(10L);
                }

                @Test
                void merchantNotFound_throws() {
                        when(merchantQueryRepo.findMerchantById(anyLong())).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.create(createReq()).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Merchant not found");
                }
        }

        // ---------- update ----------
        @Nested
        @DisplayName("update tests")
        class UpdateTests {
                @Test
                void success() {
                        UpdateMerchantDocumentRequest req = new UpdateMerchantDocumentRequest();
                        req.setDocumentId(10L);
                        req.setMerchantId(1L);
                        req.setDocumentType("PASSPORT");
                        req.setDocumentUrl("http://docs.com/passport.jpg");
                        req.setStatus("APPROVED");

                        when(documentQueryRepo.findDocumentById(10L))
                                        .thenReturn(Uni.createFrom().item(createMockDocument(10L)));
                        when(merchantQueryRepo.findMerchantById(1L))
                                        .thenReturn(Uni.createFrom().item(createMockMerchant(1L)));
                        when(documentCommandRepo.persist(any(MerchantDocument.class)))
                                        .thenReturn(Uni.createFrom().item(createPersistedDocument(10L)));

                        ApiResponse<MerchantDocumentResponse> resp = service.update(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void documentNotFound_throws() {
                        UpdateMerchantDocumentRequest req = new UpdateMerchantDocumentRequest();
                        req.setDocumentId(999L);
                        when(documentQueryRepo.findDocumentById(999L)).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.update(req).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Merchant document not found");
                }
        }

        // ---------- updateStatus ----------
        @Nested
        @DisplayName("updateStatus tests")
        class UpdateStatusTests {
                @Test
                void success() {
                        UpdateMerchantDocumentStatus req = new UpdateMerchantDocumentStatus();
                        req.setDocumentId(10L);
                        req.setMerchantId(1L);
                        req.setStatus("APPROVED");

                        when(documentQueryRepo.findDocumentById(10L))
                                        .thenReturn(Uni.createFrom().item(createMockDocument(10L)));
                        when(merchantQueryRepo.findMerchantById(1L))
                                        .thenReturn(Uni.createFrom().item(createMockMerchant(1L)));
                        when(documentCommandRepo.persist(any(MerchantDocument.class)))
                                        .thenReturn(Uni.createFrom().item(createPersistedDocument(10L)));

                        ApiResponse<MerchantDocumentResponse> resp = service.updateStatus(req).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void documentNotFound_throws() {
                        UpdateMerchantDocumentStatus req = new UpdateMerchantDocumentStatus();
                        req.setDocumentId(999L);
                        when(documentQueryRepo.findDocumentById(999L)).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.updateStatus(req).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // ---------- trash ----------
        @Nested
        @DisplayName("trash tests")
        class TrashTests {
                @Test
                void success() {
                        MerchantDocument doc = createMockDocument(10L);
                        doc.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
                        when(documentCommandRepo.trashed(10L)).thenReturn(Uni.createFrom().item(doc));

                        ApiResponse<MerchantDocumentResponseDeleteAt> resp = service.trash(10L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data().getDeletedAt()).isNotNull();
                }

                @Test
                void notFound_throws() {
                        when(documentCommandRepo.trashed(10L)).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.trash(10L).await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class)
                                        .hasMessageContaining("Merchant document not found");
                }
        }

        // ---------- restore ----------
        @Nested
        @DisplayName("restore tests")
        class RestoreTests {
                @Test
                void success() {
                        when(documentCommandRepo.restore(10L))
                                        .thenReturn(Uni.createFrom().item(createMockDocument(10L)));
                        ApiResponse<MerchantDocumentResponseDeleteAt> resp = service.restore(10L).await()
                                        .indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void notFoundOrNotTrashed_throws() {
                        when(documentCommandRepo.restore(10L)).thenReturn(Uni.createFrom().nullItem());
                        assertThatThrownBy(() -> service.restore(10L).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed first");
                }
        }

        // ---------- deletePermanent ----------
        @Nested
        @DisplayName("deletePermanent tests")
        class DeletePermanentTests {
                @Test
                void success() {
                        when(documentCommandRepo.deletePermanent(10L)).thenReturn(Uni.createFrom().item(true));
                        ApiResponse<Boolean> resp = service.deletePermanent(10L).await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data()).isTrue();
                }

                @Test
                void notTrashed_throws() {
                        when(documentCommandRepo.deletePermanent(10L)).thenReturn(Uni.createFrom().item(false));
                        assertThatThrownBy(() -> service.deletePermanent(10L).await().indefinitely())
                                        .isInstanceOf(InvalidRequestException.class)
                                        .hasMessageContaining("must be trashed before permanent deletion");
                }
        }

        // ---------- restoreAll ----------
        @Nested
        @DisplayName("restoreAll tests")
        class RestoreAllTests {
                @Test
                void success() {
                        ApiResponse<Boolean> resp = service.restoreAll().await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void noTrashed_throws() {
                        when(documentCommandRepo.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));
                        assertThatThrownBy(() -> service.restoreAll().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }

        // ---------- deleteAllPermanent ----------
        @Nested
        @DisplayName("deleteAllPermanent tests")
        class DeleteAllPermanentTests {
                @Test
                void success() {
                        ApiResponse<Boolean> resp = service.deleteAllPermanent().await().indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                }

                @Test
                void noTrashed_throws() {
                        when(documentCommandRepo.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));
                        assertThatThrownBy(() -> service.deleteAllPermanent().await().indefinitely())
                                        .isInstanceOf(ResourceNotFoundException.class);
                }
        }
}