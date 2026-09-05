package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.response.BillingStatementResponse;
import com.sanedge.card.entity.BillingStatement;
import com.sanedge.card.repository.BillingStatementRepository;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.observability.TracingMetrics;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class BillingEngineServiceImplTest {

    @Mock
    private BillingStatementRepository billingRepo;
    @Mock
    private KafkaService kafkaService;

    private TracingMetrics tracingMetrics;

    private BillingEngineServiceImpl service;

    @BeforeEach
    void setUp() {
        tracingMetrics = mock(TracingMetrics.class, withSettings().lenient());

        service = new BillingEngineServiceImpl();
        service.billingRepo = billingRepo;
        service.kafkaService = kafkaService;
        service.tracingMetrics = tracingMetrics;

        // Lenient stubs for traceAndMeasure - handle both 3-param and 4-param overloads
        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());

        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(io.opentelemetry.api.common.Attributes.class), any());
    }

    private BillingStatement sampleStatement() {
        BillingStatement stmt = new BillingStatement();
        stmt.statementId = 1L;
        stmt.cardNumber = "CARD-1";
        stmt.statementDate = Date.valueOf(LocalDate.of(2024, 6, 1));
        stmt.closingBalance = new BigDecimal("500000");
        stmt.minimumPayment = new BigDecimal("50000");
        stmt.status = "OPEN";
        return stmt;
    }

    @Nested
    @DisplayName("triggerBillingCycle tests")
    class TriggerBillingCycleTests {
        @Test
        void successProcessesStatements() {
            BillingStatement stmt = sampleStatement();
            when(billingRepo.findByBillingCycleDay(15)).thenReturn(Uni.createFrom().item(List.of(stmt)));
            when(billingRepo.persist(any(List.class))).thenReturn(Uni.createFrom().voidItem());
            when(kafkaService.sendMessage(anyString(), anyString(), any())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<Integer> resp = service.triggerBillingCycle(15).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).isEqualTo(1);
        }

        @Test
        void noOpenStatementsReturnsZero() {
            when(billingRepo.findByBillingCycleDay(10)).thenReturn(Uni.createFrom().item(List.of()));

            ApiResponse<Integer> resp = service.triggerBillingCycle(10).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.message()).contains("No open statements");
            assertThat(resp.data()).isZero();
        }

        @Test
        void failureReturnsErrorResponse() {
            when(billingRepo.findByBillingCycleDay(20))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("DB error")));
            ApiResponse<Integer> resp = service.triggerBillingCycle(20).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.data()).isZero();
        }
    }

    @Nested
    @DisplayName("getStatement tests")
    class GetStatementTests {
        @Test
        void found() {
            when(billingRepo.findByCardNumberAndStatementDate("CARD-1", Date.valueOf(LocalDate.of(2024, 6, 1))))
                    .thenReturn(Uni.createFrom().item(sampleStatement()));

            ApiResponse<BillingStatementResponse> resp = service.getStatement("CARD-1", LocalDate.of(2024, 6, 1))
                    .await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getCardNumber()).isEqualTo("CARD-1");
        }

        @Test
        void notFoundReturnsError() {
            when(billingRepo.findByCardNumberAndStatementDate(any(), any())).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<BillingStatementResponse> resp = service.getStatement("x", LocalDate.now()).await()
                    .indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Statement not found");
        }
    }

    @Nested
    @DisplayName("getStatementsByCard tests")
    class GetStatementsByCardTests {
        @Test
        void success() {
            when(billingRepo.findByCardNumber("CARD-1", 1, 10))
                    .thenReturn(Uni.createFrom().item(List.of(sampleStatement())));
            when(billingRepo.countByCardNumber("CARD-1")).thenReturn(Uni.createFrom().item(1L));

            ApiResponsePagination<List<BillingStatementResponse>> resp = service.getStatementsByCard("CARD-1", 1, 10)
                    .await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data()).hasSize(1);
        }

        @Test
        void failureReturnsError() {
            when(billingRepo.findByCardNumber(any(), anyInt(), anyInt()))
                    .thenReturn(Uni.createFrom().failure(new RuntimeException("fail")));
            when(billingRepo.countByCardNumber(any())).thenReturn(Uni.createFrom().item(0L));

            ApiResponsePagination<List<BillingStatementResponse>> resp = service.getStatementsByCard("CARD-1", 1, 10)
                    .await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.data()).isEmpty();
        }
    }
}