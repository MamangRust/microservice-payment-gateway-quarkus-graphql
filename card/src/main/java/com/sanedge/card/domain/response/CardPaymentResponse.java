package com.sanedge.card.domain.response;

import java.math.BigDecimal;

import com.sanedge.card.entity.CardPayment;

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
public class CardPaymentResponse {
    private Long paymentId;
    private String cardNumber;
    private Long statementId;
    private BigDecimal amount;
    private String paymentChannel;
    private String referenceId;
    private String status;
    private String paidAt;
    private String createdAt;
    private String updatedAt;

    public static CardPaymentResponse from(CardPayment payment) {
        if (payment == null) {
            return null;
        }
        return CardPaymentResponse.builder()
                .paymentId(payment.paymentId)
                .cardNumber(payment.cardNumber)
                .statementId(payment.statementId)
                .amount(payment.amount)
                .paymentChannel(payment.paymentChannel)
                .referenceId(payment.referenceId)
                .status(payment.status)
                .paidAt(payment.paidAt != null ? payment.paidAt.toString() : null)
                .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null)
                .updatedAt(payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : null)
                .build();
    }
}
