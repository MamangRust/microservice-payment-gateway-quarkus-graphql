package com.sanedge.card.repository;

import java.math.BigDecimal;
import java.util.List;

import com.sanedge.card.entity.CardReward;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardRewardRepository implements PanacheRepository<CardReward> {

    @WithTransaction
    public Uni<List<CardReward>> findByCardNumber(String cardNumber, int page, int size) {
        return find("cardNumber = ?1 AND deletedAt IS NULL ORDER BY createdAt DESC", cardNumber)
                .page(Page.of(page - 1, size))
                .list();
    }

    @WithTransaction
    public Uni<Long> countByCardNumber(String cardNumber) {
        return count("cardNumber = ?1 AND deletedAt IS NULL", cardNumber);
    }

    @WithTransaction
    public Uni<BigDecimal> sumPointsByCardNumber(String cardNumber) {
        return find("cardNumber = ?1 AND deletedAt IS NULL AND redeemed = FALSE", cardNumber)
                .list()
                .map(list -> list.stream()
                        .map(r -> r.pointsEarned)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
}
