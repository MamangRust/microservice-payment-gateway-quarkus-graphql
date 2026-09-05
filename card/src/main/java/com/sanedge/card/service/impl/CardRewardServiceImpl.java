package com.sanedge.card.service.impl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.domain.response.CardRewardResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.card.entity.CardReward;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.repository.CardRewardRepository;
import com.sanedge.card.service.CardRewardService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CardRewardServiceImpl implements CardRewardService {

    private static final Logger logger = LoggerFactory.getLogger(CardRewardServiceImpl.class);

    @Inject
    CardRewardRepository rewardRepo;

    @Inject
    CardCommandRepository cardCommandRepo;

    @Inject
    CardQueryRepository cardQueryRepository;

    @Inject
    TracingMetrics tracingMetrics;

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardRewardResponse>> earnRewards(
            String cardNumber, Long authTxnId, BigDecimal amount, String mcc) {
        Attributes attrs = Attributes.builder()
                .put("card.number", cardNumber)
                .put("amount", amount.toString())
                .build();

        return tracingMetrics.traceAndMeasure("earnRewards", "card_rewards", attrs, () -> {
            return cardQueryRepository.findByCardNumber(cardNumber)
                    .chain(card -> {
                        if (card == null) {
                            return Uni.createFrom().item(
                                    new ApiResponse<CardRewardResponse>("error", "Card not found", null));
                        }

                        BigDecimal pointsRate = getPointsRate(mcc);
                        BigDecimal pointsEarned = amount.multiply(pointsRate);
                        Instant expiresAt = java.time.ZonedDateTime.now().plusMonths(12).toInstant();

                        CardReward reward = new CardReward();
                        reward.cardNumber = cardNumber;
                        reward.authTxnId = authTxnId;
                        reward.amount = amount;
                        reward.mcc = mcc;
                        reward.pointsEarned = pointsEarned;
                        reward.expiresAt = expiresAt;
                        reward.redeemed = false;

                        return rewardRepo.persist(reward)
                                .map(savedReward -> {
                                    card.points = card.points.add(pointsEarned);
                                    cardCommandRepo.persist(card)
                                            .subscribe().with(v -> {
                                            },
                                                    err -> logger.error("Failed to update card points", err));

                                    return new ApiResponse<>("success",
                                            pointsEarned + " points earned",
                                            CardRewardResponse.from(savedReward));
                                });
                    });
        }).onFailure().recoverWithItem(e -> {
            logger.error("Earn rewards failed for card={}", cardNumber, e);
            return new ApiResponse<>("error", "Failed to earn rewards: " + e.getMessage(), null);
        });
    }

    @Override
    public Uni<ApiResponse<BigDecimal>> getBalance(String cardNumber) {
        return rewardRepo.sumPointsByCardNumber(cardNumber)
                .map(balance -> new ApiResponse<>("success", "Reward balance retrieved", balance))
                .onFailure().recoverWithItem(e -> {
                    logger.error("Failed to get reward balance for card={}", cardNumber, e);
                    return new ApiResponse<>("error", "Failed to get reward balance", BigDecimal.ZERO);
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<CardRewardResponse>>> getHistory(
            String cardNumber, int page, int size) {
        Uni<List<CardReward>> listUni = rewardRepo.findByCardNumber(cardNumber, page, size);
        Uni<Long> countUni = rewardRepo.countByCardNumber(cardNumber);

        return Uni.combine().all().unis(listUni, countUni).asTuple()
                .map(tuple -> {
                    List<CardReward> rewards = tuple.getItem1();
                    Long totalRecords = tuple.getItem2();

                    int totalPages = (int) Math.ceil((double) totalRecords / size);
                    PaginationMeta pagination = new PaginationMeta(page, size, totalPages,
                            totalRecords.intValue());

                    List<CardRewardResponse> data = rewards.stream()
                            .map(CardRewardResponse::from)
                            .collect(Collectors.toList());

                    return new ApiResponsePagination<>("success", "Reward history retrieved", data, pagination);
                })
                .onFailure().recoverWithItem(e -> {
                    logger.error("Failed to get reward history for card={}", cardNumber, e);
                    return new ApiResponsePagination<>("error", "Failed to get reward history",
                            List.of(), new PaginationMeta(page, size, 0, 0));
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardRewardResponse>> redeemRewards(String cardNumber, BigDecimal points) {
        return cardQueryRepository.findByCardNumber(cardNumber)
                .chain(card -> {
                    if (card == null) {
                        return Uni.createFrom().item(
                                new ApiResponse<CardRewardResponse>("error", "Card not found", null));
                    }
                    if (card.points.compareTo(points) < 0) {
                        return Uni.createFrom().item(
                                new ApiResponse<CardRewardResponse>("error",
                                        "Insufficient reward points. Available: " + card.points, null));
                    }

                    return rewardRepo.findByCardNumber(cardNumber, 1, 100)
                            .chain(rewards -> {
                                BigDecimal toRedeem = points;
                                for (CardReward reward : rewards) {
                                    if (toRedeem.compareTo(BigDecimal.ZERO) <= 0)
                                        break;
                                    if (!reward.redeemed) {
                                        reward.redeemed = true;
                                        toRedeem = toRedeem.subtract(reward.pointsEarned);
                                    }
                                }

                                return rewardRepo.persist(rewards)
                                        .chain(saved -> {
                                            card.points = card.points.subtract(points);
                                            CardReward lastReward = rewards.get(rewards.size() - 1);
                                            return cardCommandRepo.persist(card)
                                                    .map(v -> new ApiResponse<>("success",
                                                            points + " points redeemed successfully",
                                                            CardRewardResponse.from(lastReward)));
                                        });
                            });
                })
                .onFailure().recoverWithItem(e -> {
                    logger.error("Redeem rewards failed for card={}", cardNumber, e);
                    return new ApiResponse<>("error", "Failed to redeem rewards: " + e.getMessage(), null);
                });
    }

    private BigDecimal getPointsRate(String mcc) {
        Set<String> bonusMcc = Set.of("5812", "5813", "5311");
        if (mcc != null && bonusMcc.contains(mcc)) {
            return new BigDecimal("0.02");
        }
        return new BigDecimal("0.01");
    }
}
