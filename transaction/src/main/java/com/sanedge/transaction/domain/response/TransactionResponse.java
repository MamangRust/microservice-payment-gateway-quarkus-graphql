package com.sanedge.transaction.domain.response;

import com.sanedge.transaction.entity.Transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String transactionNo;
    private String cardNumber;
    private Long amount;
    private String paymentMethod;
    private Long merchantId;
    private String transactionTime;
    private String createdAt;
    private String updatedAt;

    public static TransactionResponse from(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getTransactionId())
                .transactionNo(tx.getTransactionNo().toString())
                .cardNumber(tx.getCardNumber())
                .amount(tx.getAmount().longValue())
                .paymentMethod(tx.getPaymentMethod())
                .merchantId(tx.getMerchantId().longValue())
                .transactionTime(tx.getTransactionTime() != null ? tx.getTransactionTime().toString() : null)
                .createdAt(tx.getCreatedAt() != null ? tx.getCreatedAt().toString() : null)
                .updatedAt(tx.getUpdatedAt() != null ? tx.getUpdatedAt().toString() : null)
                .build();
    }
}