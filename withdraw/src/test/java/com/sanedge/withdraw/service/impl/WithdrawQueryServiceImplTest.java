package com.sanedge.withdraw.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.repository.WithdrawQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class WithdrawQueryServiceImplTest {

    @Mock
    private WithdrawQueryRepository withdrawQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    // FIX: Hapus "@Mock" di sini karena kita instantiate real object di setUp()
    private WithdrawQueryServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new WithdrawQueryServiceImpl(
                withdrawQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    private Withdraw createMockWithdraw(Long id) {
        Withdraw w = new Withdraw();
        w.withdrawId = id;
        w.setWithdrawNo(UUID.randomUUID()); // FIX: NPE getWithdrawNo().toString()
        w.setCardNumber("1234567890123456");
        w.setWithdrawAmount(150000);
        w.setStatus(Status.PENDING);
        w.setWithdrawTime(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30, 0)));
        w.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        w.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return w;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize in test helper", e);
        }
    }

    @Nested
    @DisplayName("findAllWithdraws tests")
    class FindAllTests {

        @Test
        @DisplayName("cache miss - should fetch from DB and cache result")
        void cacheMiss_fetchesFromDb() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllWithdraws(any(FindAllWithdraws.class)))
                    .thenReturn(Uni.createFrom().item(
                            new PagedResult<>(List.of(createMockWithdraw(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAll(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - should return cached response without DB call")
        void cacheHit_returnsCached() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            WithdrawResponse cachedData = WithdrawResponse.from(createMockWithdraw(1L));
            ApiResponsePagination<List<WithdrawResponse>> cachedResponse = new ApiResponsePagination<>(
                    "success", "Withdraws retrieved successfully", List.of(cachedData), null);

            when(redisService.getReactive("withdraws:all:0:10:"))
                    .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAll(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache miss with search - should pass search parameter to repository")
        void cacheMiss_withSearchParameter() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);
            req.setSearch("1234");

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllWithdraws(any(FindAllWithdraws.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAll(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveWithdraws tests")
    class FindByActiveTests {

        @Test
        @DisplayName("cache miss - should fetch active withdraws from DB")
        void cacheMiss_fetchesFromDb() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findActiveWithdraws(any(FindAllWithdraws.class)))
                    .thenReturn(Uni.createFrom().item(
                            new PagedResult<>(List.of(createMockWithdraw(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponseDeleteAt>> result = service.findByActive(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - should return cached active withdraws")
        void cacheHit_returnsCached() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            WithdrawResponseDeleteAt cachedData = WithdrawResponseDeleteAt.from(createMockWithdraw(1L));
            ApiResponsePagination<List<WithdrawResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                    "success", "Active withdraws retrieved successfully", List.of(cachedData), null);

            when(redisService.getReactive(anyString()))
                    .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<WithdrawResponseDeleteAt>> result = service.findByActive(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findTrashedWithdraws tests")
    class FindByTrashedTests {

        @Test
        @DisplayName("cache miss - should fetch trashed withdraws from DB")
        void cacheMiss_fetchesFromDb() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findTrashedWithdraws(any(FindAllWithdraws.class)))
                    .thenReturn(Uni.createFrom().item(
                            new PagedResult<>(List.of(createMockWithdraw(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponseDeleteAt>> result = service.findByTrashed(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - should return cached trashed withdraws")
        void cacheHit_returnsCached() {
            FindAllWithdraws req = new FindAllWithdraws();
            req.setPage(1);
            req.setPageSize(10);

            WithdrawResponseDeleteAt cachedData = WithdrawResponseDeleteAt.from(createMockWithdraw(1L));
            ApiResponsePagination<List<WithdrawResponseDeleteAt>> cachedResponse = new ApiResponsePagination<>(
                    "success", "Trashed withdraws retrieved successfully", List.of(cachedData), null);

            when(redisService.getReactive(anyString()))
                    .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<WithdrawResponseDeleteAt>> result = service.findByTrashed(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByCardNumber tests")
    class FindByCardNumberTests {

        @Test
        @DisplayName("cache miss - should fetch withdraws by card number from DB")
        void cacheMiss_fetchesFromDb() {
            String cardNumber = "1234567890123456";
            FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
            req.setPage(1);
            req.setPageSize(10);
            req.setCardNumber(cardNumber);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllByCardNumber(any(FindAllWithdrawCardNumber.class)))
                    .thenReturn(Uni.createFrom().item(
                            new PagedResult<>(List.of(createMockWithdraw(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAllByCardNumber(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache miss - should return empty list when no withdraws found")
        void cacheMiss_returnsEmptyList() {
            String cardNumber = "9999999999999999";
            FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
            req.setPage(1);
            req.setPageSize(10);
            req.setCardNumber(cardNumber);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllByCardNumber(any(FindAllWithdrawCardNumber.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAllByCardNumber(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByCardNumber tests")
    class FindAllByCardNumberTests {

        @Test
        @DisplayName("cache miss - should fetch paged withdraws by card number from DB")
        void cacheMiss_fetchesFromDb() {
            FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
            req.setCardNumber("1234567890123456");
            req.setPage(1);
            req.setPageSize(10);

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllByCardNumber(any(FindAllWithdrawCardNumber.class)))
                    .thenReturn(Uni.createFrom().item(
                            new PagedResult<>(List.of(createMockWithdraw(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAllByCardNumber(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache hit - should return cached paged withdraws by card number")
        void cacheHit_returnsCached() {
            FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
            req.setCardNumber("1234567890123456");
            req.setPage(1);
            req.setPageSize(10);

            WithdrawResponse cachedData = WithdrawResponse.from(createMockWithdraw(1L));
            ApiResponsePagination<List<WithdrawResponse>> cachedResponse = new ApiResponsePagination<>(
                    "success", "Withdraws by card number retrieved successfully",
                    List.of(cachedData), null);

            when(redisService.getReactive(anyString()))
                    .thenReturn(Uni.createFrom().item(toJson(cachedResponse)));

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAllByCardNumber(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        @DisplayName("cache miss with search - should pass search parameter to repository")
        void cacheMiss_withSearchParameter() {
            FindAllWithdrawCardNumber req = new FindAllWithdrawCardNumber();
            req.setCardNumber("1234567890123456");
            req.setPage(1);
            req.setPageSize(10);
            req.setSearch("150000");

            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findAllByCardNumber(any(FindAllWithdrawCardNumber.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(), 0)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<WithdrawResponse>> result = service.findAllByCardNumber(req)
                    .await()
                    .indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {

        @Test
        @DisplayName("cache miss - should fetch withdraw by id from DB")
        void cacheMiss_fetchesFromDb() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findById(anyLong()))
                    .thenReturn(Uni.createFrom().item(createMockWithdraw(1L)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponse<WithdrawResponse> result = service.findById(1L).await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("cache hit - should return cached without DB call")
        void cacheHit_returnsCached() {
            WithdrawResponse cached = WithdrawResponse.from(createMockWithdraw(1L));

            when(redisService.getReactive("withdraws:id:1"))
                    .thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponse<WithdrawResponse> result = service.findById(1L).await().indefinitely();

            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should fail when withdraw not found")
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(withdrawQueryRepository.findById(anyLong()))
                    .thenReturn(Uni.createFrom().nullItem());

            ApiResponse<WithdrawResponse> result = service.findById(999L).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Withdraw not found");
        }
    }
}