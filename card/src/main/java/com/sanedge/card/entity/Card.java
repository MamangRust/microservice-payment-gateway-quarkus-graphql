package com.sanedge.card.entity;

import java.math.BigDecimal;
import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class Card extends BaseModel {
    public enum CardStatus {
        ACTIVE, SUSPENDED, BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    public Long cardId;

    @Column(name = "user_id", nullable = false)
    public Integer userId;

    @Column(name = "card_number", unique = true, nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "card_type", nullable = false)
    public String cardType;

    @Column(name = "expire_date", nullable = false)
    public Date expireDate;

    @Column(nullable = false, length = 3)
    public String cvv;

    @Column(name = "card_provider", nullable = false)
    public String cardProvider;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    public CardStatus status = CardStatus.ACTIVE;

    @Column(name = "credit_limit", precision = 19, scale = 2)
    public BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    public BigDecimal points = BigDecimal.ZERO;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Card))
            return false;
        return cardId != null && cardId.equals(((Card) o).cardId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
