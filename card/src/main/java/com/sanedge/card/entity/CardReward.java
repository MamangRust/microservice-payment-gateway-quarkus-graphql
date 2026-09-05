package com.sanedge.card.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "card_rewards")
public class CardReward extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    public Long rewardId;

    @Column(name = "card_number", nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "auth_txn_id")
    public Long authTxnId;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(length = 4)
    public String mcc;

    @Column(name = "points_earned", precision = 19, scale = 2)
    public BigDecimal pointsEarned = BigDecimal.ZERO;

    @Column(name = "expires_at")
    public Instant expiresAt;

    @Column
    public Boolean redeemed = false;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CardReward))
            return false;
        return rewardId != null && rewardId.equals(((CardReward) o).rewardId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
