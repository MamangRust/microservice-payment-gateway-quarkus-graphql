package com.sanedge.card.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.card.domain.response.CardRewardResponse;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.entity.CardReward;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.repository.CardRewardRepository;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class CardRewardServiceImplTest {

    @Mock
    private CardRewardRepository rewardRepo;
    @Mock
    private CardCommandRepository cardCommandRepo;
    @Mock
    private CardQueryRepository cardQueryRepository;

    private TracingMetrics tracingMetrics;

    private CardRewardServiceImpl service;

    @BeforeEach
    void setUp() {
        tracingMetrics = mock(TracingMetrics.class, withSettings().lenient());

        service = new CardRewardServiceImpl();
        service.rewardRepo = rewardRepo;
        service.cardCommandRepo = cardCommandRepo;
        service.cardQueryRepository = cardQueryRepository;
        service.tracingMetrics = tracingMetrics;

        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

        lenient().doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Uni<?>> s = inv.getArgument(inv.getArguments().length - 1);
            return s.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
    }

        private Card activeCard() {
                Card c = new Card();
                c.cardNumber = "CARD-REW";
                c.status = CardStatus.ACTIVE;
                c.points = new BigDecimal("500");
                c.creditLimit = BigDecimal.ZERO;
                return c;
        }

        private CardReward reward() {
                CardReward r = new CardReward();
                r.rewardId = 1L;
                r.cardNumber = "CARD-REW";
                r.pointsEarned = new BigDecimal("100");
                r.redeemed = false;
                return r;
        }

        @Nested
        @DisplayName("earnRewards tests")
        class EarnRewardsTests {
                @Test
                void success() {
                        when(cardQueryRepository.findByCardNumber("CARD-REW"))
                                        .thenReturn(Uni.createFrom().item(activeCard()));
                        when(rewardRepo.persist(any(CardReward.class))).thenAnswer(inv -> {
                                CardReward r = inv.getArgument(0);
                                r.rewardId = 1L;
                                return Uni.createFrom().item(r);
                        });

                        ApiResponse<CardRewardResponse> resp = service
                                        .earnRewards("CARD-REW", 100L, new BigDecimal("5000"), "5812").await()
                                        .indefinitely();
                        assertThat(resp.status()).isEqualTo("success");
                        assertThat(resp.data().getPointsEarned()).isEqualByComparingTo(new BigDecimal("100.00")); // 5000*0.02
                                                                                                                  // for
                                                                                                                  // bonus
                                                                                                                  // mcc
                }

                @Test
                void cardNotFound() {
                        when(cardQueryRepository.findByCardNumber("INVALID")).thenReturn(Uni.createFrom().nullItem());
                        ApiResponse<CardRewardResponse> resp = service
                                        .earnRewards("INVALID", 1L, BigDecimal.ONE, "0000").await().indefinitely();
                        assertThat(resp.status()).isEqualTo("error");
                        assertThat(resp.message()).contains("Card not found");
                }
        }

        @Test
        @DisplayName("getBalance returns sum of points")
        void getBalanceSuccess() {
                when(rewardRepo.sumPointsByCardNumber("CARD-REW"))
                                .thenReturn(Uni.createFrom().item(new BigDecimal("250")));
                ApiResponse<BigDecimal> resp = service.getBalance("CARD-REW").await().indefinitely();
                assertThat(resp.data()).isEqualByComparingTo("250");
        }

        @Test
        @DisplayName("getHistory returns paginated rewards")
        void getHistorySuccess() {
                when(rewardRepo.findByCardNumber("CARD-REW", 1, 10))
                                .thenReturn(Uni.createFrom().item(List.of(reward())));
                when(rewardRepo.countByCardNumber("CARD-REW")).thenReturn(Uni.createFrom().item(1L));

                ApiResponsePagination<List<CardRewardResponse>> resp = service.getHistory("CARD-REW", 1, 10).await()
                                .indefinitely();
                assertThat(resp.status()).isEqualTo("success");
                assertThat(resp.data()).hasSize(1);
        }

        @Test
        @DisplayName("redeemRewards success")
        void redeemRewardsSuccess() {
                Card card = activeCard();
                when(cardQueryRepository.findByCardNumber("CARD-REW")).thenReturn(Uni.createFrom().item(card));
                when(rewardRepo.findByCardNumber("CARD-REW", 1, 100))
                                .thenReturn(Uni.createFrom().item(List.of(reward())));
                when(rewardRepo.persist(any(List.class))).thenReturn(Uni.createFrom().voidItem());

                when(cardCommandRepo.persist(any(Card.class))).thenReturn(Uni.createFrom().item(card));

                ApiResponse<CardRewardResponse> resp = service.redeemRewards("CARD-REW", new BigDecimal("100")).await()
                                .indefinitely();
                assertThat(resp.status()).isEqualTo("success");
        }

        @Test
        @DisplayName("redeemRewards insufficient points")
        void redeemRewardsInsufficient() {
                Card card = activeCard();
                card.points = new BigDecimal("10");
                when(cardQueryRepository.findByCardNumber("CARD-REW")).thenReturn(Uni.createFrom().item(card));

                ApiResponse<CardRewardResponse> resp = service.redeemRewards("CARD-REW", new BigDecimal("500")).await()
                                .indefinitely();
                assertThat(resp.status()).isEqualTo("error");
                assertThat(resp.message()).contains("Insufficient");
        }
}