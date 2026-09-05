package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.requests.AuthorizeCardRequest;
import com.sanedge.card.domain.requests.ReverseTransactionRequest;
import com.sanedge.card.domain.response.CardAuthTransactionResponse;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.entity.CardAuthTransaction;
import com.sanedge.card.entity.CardAuthTransaction.AuthTxnStatus;
import com.sanedge.card.repository.CardAuthTransactionRepository;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CardAuthServiceImplTest {

    @Mock
    private CardAuthTransactionRepository authTxnRepo;
    @Mock
    private CardCommandRepository cardCommandRepo;
    @Mock
    private CardQueryRepository cardQueryRepository;
    @Mock
    private KafkaService kafkaService;
    @Mock
    private TracingMetrics tracingMetrics;
    @Mock
    private Validator validator;

    private CardAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CardAuthServiceImpl();
        service.authTxnRepo = authTxnRepo;
        service.cardCommandRepo = cardCommandRepo;
        service.cardQueryRepository = cardQueryRepository;
        service.kafkaService = kafkaService;
        service.tracingMetrics = tracingMetrics;
        service.validator = validator;

        // 3-arg traceAndMeasure used in this service
        lenient().doAnswer(inv -> {
            Supplier<Uni<?>> s = inv.getArgument(3);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().when(kafkaService.sendMessage(anyString(), anyString(), any()))
                .thenReturn(Uni.createFrom().voidItem());
    }

    private Card activeCard() {
        Card c = new Card();
        c.cardId = 1L;
        c.cardNumber = "CARD-1";
        c.status = CardStatus.ACTIVE;
        c.creditLimit = new BigDecimal("10000000");
        c.points = BigDecimal.ZERO;
        return c;
    }

    private CardAuthTransaction pendingTxn(Long id) {
        CardAuthTransaction txn = new CardAuthTransaction();
        txn.authTxnId = id;
        txn.cardNumber = "CARD-1";
        txn.merchantId = 10;
        txn.amount = new BigDecimal("500000");
        txn.currency = "IDR";
        txn.idempotencyKey = "idem-1";
        txn.status = AuthTxnStatus.PENDING;
        return txn;
    }

    private AuthorizeCardRequest authReq() {
        AuthorizeCardRequest req = new AuthorizeCardRequest();
        req.setCardNumber("CARD-1");
        req.setMerchantId(10);
        req.setAmount(new BigDecimal("500000"));
        req.setCurrency("IDR");
        req.setIdempotencyKey("idem-1");
        req.setPosEntryMode("021");
        req.setMcc("5812");
        return req;
    }

    @Nested
    @DisplayName("authorize tests")
    class AuthorizeTests {
        @Test
        void successApproved() {
            AuthorizeCardRequest req = authReq();
            when(authTxnRepo.findByIdempotencyKey("idem-1")).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findByCardNumber("CARD-1")).thenReturn(Uni.createFrom().item(activeCard()));
            when(authTxnRepo.persist(any(CardAuthTransaction.class))).thenAnswer(inv -> {
                CardAuthTransaction t = inv.getArgument(0);
                t.authTxnId = 100L;
                return Uni.createFrom().item(t);
            });
            when(authTxnRepo.updateStatus(100L, AuthTxnStatus.APPROVED)).thenAnswer(inv -> {
                CardAuthTransaction txn = pendingTxn(100L);
                txn.status = AuthTxnStatus.APPROVED;
                return Uni.createFrom().item(txn);
            });

            ApiResponse<CardAuthTransactionResponse> resp = service.authorize(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.data().getStatus()).isEqualTo("APPROVED");
        }

        @Test
        void cardNotFound() {
            AuthorizeCardRequest req = authReq();
            when(authTxnRepo.findByIdempotencyKey(any())).thenReturn(Uni.createFrom().nullItem());
            when(cardQueryRepository.findByCardNumber("CARD-1")).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<CardAuthTransactionResponse> resp = service.authorize(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("Card not found");
        }
    }

    @Nested
    @DisplayName("reverse tests")
    class ReverseTests {
        @Test
        void success() {
            ReverseTransactionRequest req = new ReverseTransactionRequest();
            req.setAuthTxnId(100L);
            req.setCardNumber("CARD-1");

            CardAuthTransaction approved = pendingTxn(100L);
            approved.status = AuthTxnStatus.APPROVED;
            when(authTxnRepo.findById(100L)).thenReturn(Uni.createFrom().item(approved));
            when(authTxnRepo.updateStatus(100L, AuthTxnStatus.REVERSED)).thenAnswer(inv -> {
                approved.status = AuthTxnStatus.REVERSED;
                return Uni.createFrom().item(approved);
            });

            ApiResponse<CardAuthTransactionResponse> resp = service.reverse(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("success");
            assertThat(resp.message()).contains("reversed");
        }

        @Test
        void transactionNotFound() {
            ReverseTransactionRequest req = new ReverseTransactionRequest();
            req.setAuthTxnId(999L);
            when(authTxnRepo.findById(999L)).thenReturn(Uni.createFrom().nullItem());

            ApiResponse<CardAuthTransactionResponse> resp = service.reverse(req).await().indefinitely();
            assertThat(resp.status()).isEqualTo("error");
            assertThat(resp.message()).contains("not found");
        }
    }
}