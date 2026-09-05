package com.sanedge.card.domain.response;

import com.sanedge.card.entity.Card;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class CardResponseDeleteAt {
    private Long id;
    private Long userId;
    private String cardNumber;
    private String cardType;
    private String expireDate;
    private String cvv;
    private String cardProvider;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static CardResponseDeleteAt from(Card card) {
        if (card == null) {
            return null;
        }
        return CardResponseDeleteAt.builder()
                .id(card.getCardId())
                .userId(card.getUserId() != null ? card.getUserId().longValue() : null)
                .cardNumber(card.getCardNumber())
                .cardType(card.getCardType())
                .expireDate(card.getExpireDate() != null ? card.getExpireDate().toString() : null)
                .cvv(card.getCvv())
                .cardProvider(card.getCardProvider())
                .createdAt(card.getCreatedAt() != null ? card.getCreatedAt().toString() : null)
                .updatedAt(card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : null)
                .deletedAt(card.getDeletedAt() != null ? card.getDeletedAt().toString() : null)
                .build();
    }
}
