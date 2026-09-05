package com.sanedge.transfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transfer.domain.requests.FindAllTransfers;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.repository.TransferQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransferQueryServiceImplTest {

        @Mock
        private TransferQueryRepository transferQueryRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private TransferQueryServiceImpl service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                service = new TransferQueryServiceImpl(
                                transferQueryRepository,
                                redisService,
                                objectMapper,
                                tracingMetrics);

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
        }

        private Transfer createMockTransfer(Long id) {
                Transfer t = new Transfer();
                t.transferId = id;
                t.setTransferNo(UUID.randomUUID());
                t.setTransferFrom("111122223333");
                t.setTransferTo("444455556666");
                t.setTransferAmount(150000);
                t.setStatus(Status.PENDING);
                t.setTransferTime(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30, 0)));
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException("Failed to serialize in test helper", e);
                }
        }

        @Nested
        @DisplayName("findAll tests")
        class FindAllTests {

                @Test
                @DisplayName("cache miss - should fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransfers(any(FindAllTransfers.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransfer(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransferResponse>> result = service.findAll(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached response without DB call")
                void cacheHit_returnsCached() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        TransferResponse cachedData = TransferResponse.from(createMockTransfer(1L));
                        ApiResponsePagination<List<TransferResponse>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Transfers retrieved successfully", List.of(cachedData), null);

                        when(redisService.getReactive("transfers:all:0:10:"))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransferResponse>> result = service.findAll(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache miss with search - should pass search parameter to repository")
                void cacheMiss_withSearchParameter() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setSearch("1111");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransfers(any(FindAllTransfers.class)))
                                        .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransferResponse>> result = service.findAll(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).isEmpty();
                }
        }

        @Nested
        @DisplayName("findByActive tests")
        class FindByActiveTests {

                @Test
                @DisplayName("cache miss - should fetch active transfers from DB")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findActiveTransfers(any(FindAllTransfers.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransfer(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransferResponseDeleteAt>> result = service.findByActive(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached active transfers")
                void cacheHit_returnsCached() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        TransferResponseDeleteAt cachedData = TransferResponseDeleteAt.from(createMockTransfer(1L));
                        ApiResponsePagination<List<TransferResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Active transfers retrieved successfully", List.of(cachedData),
                                        null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransferResponseDeleteAt>> result = service.findByActive(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findByTrashed tests")
        class FindByTrashedTests {

                @Test
                @DisplayName("cache miss - should fetch trashed transfers from DB")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTrashedTransfers(any(FindAllTransfers.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransfer(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransferResponseDeleteAt>> result = service.findByTrashed(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached trashed transfers")
                void cacheHit_returnsCached() {
                        FindAllTransfers req = new FindAllTransfers();
                        req.setPage(1);
                        req.setPageSize(10);

                        TransferResponseDeleteAt cachedData = TransferResponseDeleteAt.from(createMockTransfer(1L));
                        ApiResponsePagination<List<TransferResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Trashed transfers retrieved successfully", List.of(cachedData),
                                        null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransferResponseDeleteAt>> result = service.findByTrashed(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findById tests")
        class FindByIdTests {

                @Test
                @DisplayName("cache miss - should fetch transfer by id from DB")
                void cacheMiss_fetchesFromDb() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransferById(anyLong()))
                                        .thenReturn(Uni.createFrom().item(createMockTransfer(1L)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<TransferResponse> result = service.findById(1L).await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data().getId()).isEqualTo(1L);
                }

                @Test
                @DisplayName("cache hit - should return cached without DB call")
                void cacheHit_returnsCached() {
                        TransferResponse cached = TransferResponse.from(createMockTransfer(1L));

                        when(redisService.getReactive("transfers:id:1"))
                                        .thenReturn(Uni.createFrom().item(toJson(cached)));

                        ApiResponse<TransferResponse> result = service.findById(1L).await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data().getId()).isEqualTo(1L);
                }

                @Test
                @DisplayName("should fail when transfer not found")
                void notFound_throwsException() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransferById(anyLong()))
                                        .thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<TransferResponse> response = service.findById(999L).await().indefinitely();

                        assertThat(response.status()).isEqualTo("error");
                        assertThat(response.message()).contains("Transfer not found with id 999");
                }
        }

        @Nested
        @DisplayName("findByTransferFrom tests")
        class FindByTransferFromTests {

                @Test
                @DisplayName("cache miss - should fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        String cardNumber = "111122223333";

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransfersBySourceCard(anyString()))
                                        .thenReturn(Uni.createFrom().item(List.of(createMockTransfer(1L))));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<List<TransferResponse>> result = service.findByTransferFrom(cardNumber)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached response without DB call")
                void cacheHit_returnsCached() {
                        String cardNumber = "111122223333";

                        List<TransferResponse> cachedData = List.of(TransferResponse.from(createMockTransfer(1L)));
                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedData)));

                        ApiResponse<List<TransferResponse>> result = service.findByTransferFrom(cardNumber)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findByTransferTo tests")
        class FindByTransferToTests {

                @Test
                @DisplayName("cache miss - should fetch from DB and cache result")
                void cacheMiss_fetchesFromDb() {
                        String cardNumber = "444455556666";

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transferQueryRepository.findTransfersByDestinationCard(anyString()))
                                        .thenReturn(Uni.createFrom().item(List.of(createMockTransfer(1L))));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<List<TransferResponse>> result = service.findByTransferTo(cardNumber)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached response without DB call")
                void cacheHit_returnsCached() {
                        String cardNumber = "444455556666";

                        List<TransferResponse> cachedData = List.of(TransferResponse.from(createMockTransfer(1L)));
                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedData)));

                        ApiResponse<List<TransferResponse>> result = service.findByTransferTo(cardNumber)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }
}