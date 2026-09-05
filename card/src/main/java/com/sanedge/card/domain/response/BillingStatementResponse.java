package com.sanedge.card.domain.response;

import java.math.BigDecimal;

import com.sanedge.card.entity.BillingStatement;

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
public class BillingStatementResponse {
    private Long statementId;
    private String cardNumber;
    private Integer billingCycleDay;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal minimumPayment;
    private String dueDate;
    private BigDecimal fees;
    private BigDecimal interest;
    private String statementDate;
    private String status;
    private String createdAt;
    private String updatedAt;

    public static BillingStatementResponse from(BillingStatement bs) {
        if (bs == null) {
            return null;
        }
        return BillingStatementResponse.builder()
                .statementId(bs.statementId)
                .cardNumber(bs.cardNumber)
                .billingCycleDay(bs.billingCycleDay)
                .openingBalance(bs.openingBalance)
                .closingBalance(bs.closingBalance)
                .minimumPayment(bs.minimumPayment)
                .dueDate(bs.dueDate != null ? bs.dueDate.toString() : null)
                .fees(bs.fees)
                .interest(bs.interest)
                .statementDate(bs.statementDate != null ? bs.statementDate.toString() : null)
                .status(bs.status)
                .createdAt(bs.getCreatedAt() != null ? bs.getCreatedAt().toString() : null)
                .updatedAt(bs.getUpdatedAt() != null ? bs.getUpdatedAt().toString() : null)
                .build();
    }
}
