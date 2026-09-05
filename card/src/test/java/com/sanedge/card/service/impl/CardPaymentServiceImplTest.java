package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.requests.PostPaymentRequest;
import com.sanedge.card.domain.response.CardPaymentResponse;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.entity.CardPayment;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardPaymentRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CardPaymentServiceImplTest {

    @Mock
    private CardPaymentRepository paymentRepo;
    @Mock
    private CardCommandRepository cardCommandRepo;
    @Mock
    private CardQueryRepository cardQueryRepository;
    @Mock
    private KafkaService kafkaService;
    @Mock
    private TracingMetrics tracingMetrics;

    private CardPaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CardPaymentServiceImpl();
        service.paymentRepo = paymentRepo;
        service.cardCommandRepo = cardCommandRepo;
        service.cardQueryRepository = cardQueryRepository;
        service.kafkaService = kafkaService;
        service.tracingMetrics = tracingMetrics;

        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> s = inv.getArgument(3);
            return s.get();
        })
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
        lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                .thenReturn(Uni.createFrom().voidItem());
    }

    private Card activeCard() {
        Card c = new Card();
        c.cardNumber = "CARD-PAY";
        c.status = CardStatus.ACTIVE;
        return c;
    }

    private CardPayment payment() {
        CardPayment p = new CardPayment();
        p.paymentId = 1L;
        p.cardNumber = "CARD-PAY";
        p.amount = new BigDecimal("100000");
        p.status = "COMPLETED";
        p.paidAt = Instant.now();
        return p;
    }

    @Nested
    @DisplayName("postPayment tests")
    class PostPaymentTests {
        @Test
        void success() {
            PostPaymentRequest req = new PostPaymentRequest();
            req.setCardNumber("CARD-PAY");
            req.setAmount(new BigDecimal("100000"));
            when(cardQueryRepository.findByCardNumber("CARD-PAY")).thenReturn(Uni.createFrom().item(activeCard()));
            when(paymentRepo.persist(any(CardPayment.class))).thenAnswer(inv -> {
                CardPayment p = inv.getArgument(0);
                p.paymentId = 1L;
                return Uni.createFrom().item(p);
            });
            when(paymentRepo.completePayment(1L)).thenReturn(Uni.createFrom().item(payment()));

            ApiResponse<CardPaymentResponse> resp = service.postPayment(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getAmount()).isEqualByComparingTo("100000");
        }

        @Test
        void cardNotFound() {
            PostPaymentRequest req = new PostPaymentRequest();
            req.setCardNumber("INVALID");
            req.setAmount(new BigDecimal("50000"));
            when(cardQueryRepository.findByCardNumber("INVALID")).thenReturn(Uni.createFrom().nullItem());
            ApiResponse<CardPaymentResponse> resp = service.postPayment(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Card not found");
        }
    }

    @Test
    @DisplayName("getPaymentHistory returns paginated results")
    void getPaymentHistorySuccess() {
        when(paymentRepo.findByCardNumber("CARD-PAY", 1, 10)).thenReturn(Uni.createFrom().item(List.of(payment())));
        when(paymentRepo.countByCardNumber("CARD-PAY")).thenReturn(Uni.createFrom().item(1L));

        ApiResponsePagination<List<CardPaymentResponse>> resp = service.getPaymentHistory("CARD-PAY", 1, 10).await()
                .indefinitely();
        assertThat(resp.status()).isEqualTo("success");
        assertThat(resp.data()).hasSize(1);
    }

    @Test
    @DisplayName("countPayments returns count")
    void countPaymentsSuccess() {
        when(paymentRepo.countByCardNumber("CARD-PAY")).thenReturn(Uni.createFrom().item(5L));
        ApiResponse<Long> resp = service.countPayments("CARD-PAY").await().indefinitely();
        assertThat(resp.data()).isEqualTo(5L);
    }
}