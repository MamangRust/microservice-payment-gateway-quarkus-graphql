package com.sanedge.card.entity;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "card_auth_transactions")
public class CardAuthTransaction extends BaseModel {

    public enum AuthTxnStatus {
        PENDING, APPROVED, DECLINED, REVERSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_txn_id")
    public Long authTxnId;

    @Column(name = "card_number", nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "merchant_id", nullable = false)
    public Integer merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(length = 3)
    public String currency;

    @Column(name = "pos_entry_mode", length = 3)
    public String posEntryMode;

    @Column(length = 4)
    public String mcc;

    @Column(name = "idempotency_key", unique = true, length = 64)
    public String idempotencyKey;

    @Column(name = "risk_score")
    public Integer riskScore = 0;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    public AuthTxnStatus status = AuthTxnStatus.PENDING;

    @Column(name = "authorized_at")
    public Instant authorizedAt;

    @Column(name = "reversed_at")
    public Instant reversedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CardAuthTransaction))
            return false;
        return authTxnId != null && authTxnId.equals(((CardAuthTransaction) o).authTxnId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
