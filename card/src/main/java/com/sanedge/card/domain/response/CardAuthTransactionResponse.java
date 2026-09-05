package com.sanedge.card.domain.response;

import java.math.BigDecimal;
import java.time.Instant;

import com.sanedge.card.entity.CardAuthTransaction;

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
public class CardAuthTransactionResponse {
    private Long authTxnId;
    private String cardNumber;
    private Integer merchantId;
    private BigDecimal amount;
    private String currency;
    private String posEntryMode;
    private String mcc;
    private String idempotencyKey;
    private Integer riskScore;
    private String status;
    private String authorizedAt;
    private String reversedAt;
    private String createdAt;
    private String updatedAt;

    public static CardAuthTransactionResponse from(CardAuthTransaction txn) {
        if (txn == null) {
            return null;
        }
        return CardAuthTransactionResponse.builder()
                .authTxnId(txn.authTxnId)
                .cardNumber(txn.cardNumber)
                .merchantId(txn.merchantId)
                .amount(txn.amount)
                .currency(txn.currency)
                .posEntryMode(txn.posEntryMode)
                .mcc(txn.mcc)
                .idempotencyKey(txn.idempotencyKey)
                .riskScore(txn.riskScore)
                .status(txn.status != null ? txn.status.name() : null)
                .authorizedAt(txn.authorizedAt != null ? txn.authorizedAt.toString() : null)
                .reversedAt(txn.reversedAt != null ? txn.reversedAt.toString() : null)
                .createdAt(txn.getCreatedAt() != null ? txn.getCreatedAt().toString() : null)
                .updatedAt(txn.getUpdatedAt() != null ? txn.getUpdatedAt().toString() : null)
                .build();
    }
}
