package com.sanedge.transaction.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.TransactionQueryRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceImplTest {

        @Mock
        private TransactionQueryRepository transactionQueryRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private TransactionQueryServiceImpl service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                service = new TransactionQueryServiceImpl(
                                transactionQueryRepository,
                                redisService,
                                objectMapper,
                                tracingMetrics);

                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics)
                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
        }

        private Transaction createMockTransaction(Long id) {
                Transaction t = new Transaction();
                t.transactionId = id;
                t.transactionNo = UUID.randomUUID();
                t.cardNumber = "4111111111111111";
                t.amount = 150000;
                t.paymentMethod = "CREDIT_CARD";
                t.merchantId = 1;
                t.transactionTime = Timestamp.valueOf(LocalDateTime.now());
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
                        FindAllTransactions req = new FindAllTransactions();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactions(any(FindAllTransactions.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAll(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                @DisplayName("cache hit - should return cached response")
                void cacheHit_returnsCached() {
                        FindAllTransactions req = new FindAllTransactions();
                        req.setPage(1);
                        req.setPageSize(10);

                        TransactionResponse cachedData = TransactionResponse.from(createMockTransaction(1L));
                        ApiResponsePagination<List<TransactionResponse>> cachedResponse = new ApiResponsePagination<>(
                                        "success", "Transactions retrieved successfully", List.of(cachedData), null);

                        when(redisService.getReactive(anyString()))
                                        .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAll(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findById tests")
        class FindByIdTests {

                @Test
                @DisplayName("cache miss - should fetch transaction by id from DB")
                void cacheMiss_fetchesFromDb() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactionById(anyLong()))
                                        .thenReturn(Uni.createFrom().item(createMockTransaction(1L)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<TransactionResponse> result = service.findById(1L).await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).isNotNull();
                }
        }

        @Nested
        @DisplayName("findByActive tests")
        class FindByActiveTests {

                @Test
                @DisplayName("cache miss - should fetch active transactions from DB")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactions req = new FindAllTransactions();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findActiveTransactions(any(FindAllTransactions.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByActive(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findByTrashed tests")
        class FindByTrashedTests {

                @Test
                @DisplayName("cache miss - should fetch trashed transactions from DB")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactions req = new FindAllTransactions();
                        req.setPage(1);
                        req.setPageSize(10);

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTrashedTransactions(any(FindAllTransactions.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponseDeleteAt>> result = service.findByTrashed(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findByMerchantId tests")
        class FindByMerchantIdTests {

                @Test
                @DisplayName("cache miss - should fetch transactions by merchant from DB")
                void cacheMiss_fetchesFromDb() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository.findTransactionsByMerchantId(anyLong()))
                                        .thenReturn(Uni.createFrom().item(List.of(createMockTransaction(1L))));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<List<TransactionResponse>> result = service.findByMerchantId(1L)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findAllByCardNumber tests")
        class FindAllByCardNumberTests {

                @Test
                @DisplayName("cache miss - should fetch transactions by card number from DB")
                void cacheMiss_fetchesFromDb() {
                        FindAllTransactionCardNumber req = new FindAllTransactionCardNumber();
                        req.setPage(1);
                        req.setPageSize(10);
                        req.setCardNumber("4111111111111111");

                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(transactionQueryRepository
                                        .findTransactionsByCardNumber(any(FindAllTransactionCardNumber.class)))
                                        .thenReturn(Uni.createFrom().item(
                                                        new PagedResult<>(List.of(createMockTransaction(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TransactionResponse>> result = service.findAllByCardNumber(req)
                                        .await().indefinitely();

                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }
}
