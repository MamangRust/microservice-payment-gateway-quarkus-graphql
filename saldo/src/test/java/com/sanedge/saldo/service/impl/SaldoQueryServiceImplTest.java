package com.sanedge.saldo.service.impl;

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
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.saldo.domain.requests.FindAllSaldos;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.repository.SaldoQueryRepository;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class SaldoQueryServiceImplTest {

    @Mock
    private SaldoQueryRepository saldoQueryRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private SaldoQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new SaldoQueryServiceImpl(
                saldoQueryRepository,
                redisService,
                objectMapper,
                tracingMetrics);
        // Lenient stubbing to execute the supplier directly (uses 2-arg
        // traceAndMeasure)
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(2);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
    }

    private Saldo createMockSaldo(Long id) {
        Saldo s = new Saldo();
        s.setSaldoId(id);
        s.setCardNumber("1234-5678-9012-3456");
        s.setTotalBalance(500000);
        s.setWithdrawAmount(0);
        s.setCreatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30)));
        s.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return s;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllSaldos findAllReq(int page, int size, String search) {
        FindAllSaldos req = new FindAllSaldos();
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
            FindAllSaldos req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findSaldos(any(FindAllSaldos.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockSaldo(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<SaldoResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }

        @Test
        void cacheHit_returnsCached() {
            FindAllSaldos req = findAllReq(1, 10, "");
            ApiResponsePagination<List<SaldoResponse>> cached = new ApiResponsePagination<>(
                    "success", "Get all saldos success", List.of(SaldoResponse.from(createMockSaldo(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<SaldoResponse>> result = service.findAll(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findActive tests")
    class FindActiveTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllSaldos req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findActiveSaldos(any(FindAllSaldos.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockSaldo(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<SaldoResponseDeleteAt>> result = service.findActive(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findTrashed tests")
    class FindTrashedTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            FindAllSaldos req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findTrashedSaldos(any(FindAllSaldos.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockSaldo(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<SaldoResponseDeleteAt>> result = service.findTrashed(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByCard tests")
    class FindByCardTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findByCardNumber(anyString()))
                    .thenReturn(Uni.createFrom().item(createMockSaldo(1L)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponse<SaldoResponse> result = service.findByCard("1234-5678").await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getCardNumber()).isEqualTo("1234-5678-9012-3456");
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findByCardNumber(anyString())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> result = service.findByCard("nonexistent").await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Saldo not found");
        }
    }

    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test
        void cacheMiss_fetchesFromDb() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(createMockSaldo(1L)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponse<SaldoResponse> result = service.findById(1L).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(1L);
        }

        @Test
        void notFound_throwsException() {
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(saldoQueryRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<SaldoResponse> result = service.findById(999L).await().indefinitely();
            assertThat(result.status()).isEqualTo("error");
            assertThat(result.message()).contains("Saldo not found");
        }
    }
}