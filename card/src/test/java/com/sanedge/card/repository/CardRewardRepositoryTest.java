package com.sanedge.card.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sanedge.card.entity.CardReward;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class CardRewardRepositoryTest {

    @Inject
    CardRewardRepository repository;

    private Uni<CardReward> persistReward(String cardNumber, BigDecimal points, boolean redeemed) {
        CardReward reward = new CardReward();
        reward.setCardNumber(cardNumber);
        reward.setPointsEarned(points);
        reward.setRedeemed(redeemed);
        reward.setCreatedAt(Instant.now());
        reward.setUpdatedAt(Instant.now());
        return repository.persist(reward).map(r -> r);
    }

    private Uni<Void> clean() {
        return repository.deleteAll().replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumber_Paginated() {
        return clean()
                .chain(() -> persistReward("CARD-1", BigDecimal.valueOf(100), false))
                .chain(() -> persistReward("CARD-1", BigDecimal.valueOf(200), false))
                .chain(() -> repository.findByCardNumber("CARD-1", 1, 10))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    // ordered by createdAt DESC, so second inserted should be first
                    assertThat(list.get(0).getPointsEarned()).isEqualByComparingTo("200");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCountByCardNumber() {
        return clean()
                .chain(() -> persistReward("CARD-COUNT", BigDecimal.ONE, false))
                .chain(() -> repository.countByCardNumber("CARD-COUNT"))
                .invoke(count -> assertThat(count).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testSumPointsByCardNumber_OnlyNonRedeemed() {
        return clean()
                .chain(() -> persistReward("CARD-SUM", BigDecimal.valueOf(50), false))
                .chain(() -> persistReward("CARD-SUM", BigDecimal.valueOf(30), true)) // redeemed
                .chain(() -> repository.sumPointsByCardNumber("CARD-SUM"))
                .invoke(sum -> assertThat(sum).isEqualByComparingTo("50"))
                .replaceWithVoid();
    }
}