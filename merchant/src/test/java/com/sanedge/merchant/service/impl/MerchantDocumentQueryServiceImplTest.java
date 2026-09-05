package com.sanedge.merchant.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.entity.MerchantDocument;
import com.sanedge.merchant.repository.MerchantDocumentQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentQueryServiceImplTest {

    @Mock
    private MerchantDocumentQueryRepository documentQueryRepo;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private MerchantDocumentQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new MerchantDocumentQueryServiceImpl(documentQueryRepo, redisService, objectMapper, tracingMetrics);

        // Lenient stubs to execute the supplier directly
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
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

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllMerchantDocuments findAllReq(int page, int size, String search) {
        FindAllMerchantDocuments req = new FindAllMerchantDocuments();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    @Nested
    @DisplayName("findAll tests")
    class FindAllTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantDocuments req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(documentQueryRepo.findDocuments(any(FindAllMerchantDocuments.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockDocument(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDocumentResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        void cacheHit_returnsCached() {
            FindAllMerchantDocuments req = findAllReq(1, 10, "");
            ApiResponsePagination<List<MerchantDocumentResponse>> cached = new ApiResponsePagination<>(
                    "success", "Merchant documents retrieved successfully",
                    List.of(MerchantDocumentResponse.from(createMockDocument(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<MerchantDocumentResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findAllActive tests")
    class FindAllActiveTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantDocuments req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(documentQueryRepo.findActiveDocuments(any(FindAllMerchantDocuments.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockDocument(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> result = service.findAllActive(req).await()
                    .indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findAllTrashed tests")
    class FindAllTrashedTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllMerchantDocuments req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(documentQueryRepo.findTrashedDocuments(any(FindAllMerchantDocuments.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockDocument(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> result = service.findAllTrashed(req).await()
                    .indefinitely();
            assertThat(result.status()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            Long id = 1L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(documentQueryRepo.findDocumentById(anyLong()))
                    .thenReturn(Uni.createFrom().item(createMockDocument(id)));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<MerchantDocumentResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getDocumentId()).isEqualTo(id);
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(documentQueryRepo.findDocumentById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            try {
                service.findById(999L).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Merchant document not found");
            }
        }
    }
}