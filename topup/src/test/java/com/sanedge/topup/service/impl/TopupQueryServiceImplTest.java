package com.sanedge.topup.service.impl;

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
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.topup.domain.requests.FindAllTopups;
import com.sanedge.topup.domain.requests.FindAllTopupsByCardNumber;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.repository.TopupQueryRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class TopupQueryServiceImplTest {

        @Mock
        private TopupQueryRepository topupQueryRepository;

        @Mock
        private RedisService redisService;

        @Mock
        private TracingMetrics tracingMetrics;

        private TopupQueryServiceImpl service;
        private final ObjectMapper objectMapper = new ObjectMapper();

        @BeforeEach
        void setUp() {
                service = new TopupQueryServiceImpl(topupQueryRepository, redisService, objectMapper, tracingMetrics);
                // Lenient stubbing to execute the supplier directly
                lenient().doAnswer(invocation -> {
                        Supplier<Uni<?>> supplier = invocation.getArgument(3);
                        return supplier.get();
                }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
        }

        private Topup createMockTopup(Long id) {
                Topup t = new Topup();
                t.setTopupId(id);
                t.setTopupNo(UUID.randomUUID());
                t.setCardNumber("1234-5678-9012-3456");
                t.setTopupAmount(50000);
                t.setTopupMethod("BANK_TRANSFER");
                t.setStatus(Status.SUCCESS);
                t.setTopupTime(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30)));
                t.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
                t.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
                return t;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                }
        }

        @Nested
        @DisplayName("findAll tests")
        class FindAllTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        FindAllTopups req = new FindAllTopups();
                        req.setPage(1);
                        req.setPageSize(10);
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findTopups(any())).thenReturn(
                                        Uni.createFrom().item(new PagedResult<>(List.of(createMockTopup(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TopupResponse>> result = service.findAll(req).await().indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }

                @Test
                void cacheHit_returnsCached() {
                        FindAllTopups req = new FindAllTopups();
                        req.setPage(1);
                        req.setPageSize(10);
                        ApiResponsePagination<List<TopupResponse>> cached = new ApiResponsePagination<>("success",
                                        "Found", List.of(TopupResponse.from(createMockTopup(1L))), null);
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));
                        ApiResponsePagination<List<TopupResponse>> result = service.findAll(req).await().indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                }
        }

        @Nested
        @DisplayName("findAllByCardNumber tests")
        class FindAllByCardNumberTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        FindAllTopupsByCardNumber req = new FindAllTopupsByCardNumber();
                        req.setCardNumber("1234");
                        req.setPage(1);
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findTopupByCard(any())).thenReturn(
                                        Uni.createFrom().item(new PagedResult<>(List.of(createMockTopup(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TopupResponse>> result = service.findAllByCardNumber(req).await()
                                        .indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                }
        }

        @Nested
        @DisplayName("findActive tests")
        class FindActiveTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        FindAllTopups req = new FindAllTopups();
                        req.setPage(1);
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findActiveTopups(any())).thenReturn(
                                        Uni.createFrom().item(new PagedResult<>(List.of(createMockTopup(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TopupResponseDeleteAt>> result = service.findActive(req).await()
                                        .indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                }
        }

        @Nested
        @DisplayName("findTrashed tests")
        class FindTrashedTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        FindAllTopups req = new FindAllTopups();
                        req.setPage(1);
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findTrashedTopups(any())).thenReturn(
                                        Uni.createFrom().item(new PagedResult<>(List.of(createMockTopup(1L)), 1)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponsePagination<List<TopupResponseDeleteAt>> result = service.findTrashed(req).await()
                                        .indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                }
        }

        @Nested
        @DisplayName("findByCard tests")
        class FindByCardTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findByCardNumber(anyString()))
                                        .thenReturn(Uni.createFrom().item(List.of(createMockTopup(1L))));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<List<TopupResponse>> result = service.findByCard("1234").await().indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data()).hasSize(1);
                }
        }

        @Nested
        @DisplayName("findById tests")
        class FindByIdTests {
                @Test
                void cacheMiss_fetchesFromDb() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findTopupById(anyLong()))
                                        .thenReturn(Uni.createFrom().item(createMockTopup(1L)));
                        when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                                        .thenReturn(Uni.createFrom().voidItem());

                        ApiResponse<TopupResponse> result = service.findById(1L).await().indefinitely();
                        assertThat(result.status()).isEqualTo("success");
                        assertThat(result.data().getId()).isEqualTo(1L);
                }

                @Test
                void notFound_throwsException() {
                        when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                        when(topupQueryRepository.findTopupById(anyLong())).thenReturn(Uni.createFrom().nullItem());

                        ApiResponse<TopupResponse> result = service.findById(999L).await().indefinitely();
                        assertThat(result.status()).isEqualTo("error");
                        assertThat(result.message()).contains("Topup not found");
                }
        }
}