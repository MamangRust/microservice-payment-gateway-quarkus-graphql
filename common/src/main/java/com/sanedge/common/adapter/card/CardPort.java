package com.sanedge.common.adapter.card;

import com.sanedge.common.adapter.model.Card;

import io.smallrye.mutiny.Uni;

/**
 * Port for the card service: card lookups and card mutation, replacing direct
 * {@code @GrpcClient("card")} usage in consumer services.
 */
public interface CardPort {

    Uni<Card> findCardByUserId(int userId);

    /**
     * Looks up a card together with its owner's email. Used by flows that need
     * to notify the cardholder (topup, transfer, withdraw).
     */
    Uni<Card> findUserCardByCardNumber(String cardNumber);

    Uni<Card> findCardByCardNumber(String cardNumber);

    Uni<Card> updateCard(UpdateData data);

    record UpdateData(
            int cardId,
            int userId,
            String cardType,
            String expireDate,
            String cvv,
            String cardProvider) {
    }
}
