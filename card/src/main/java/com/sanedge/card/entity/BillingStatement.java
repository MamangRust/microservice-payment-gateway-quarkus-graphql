package com.sanedge.card.entity;

import java.math.BigDecimal;
import java.sql.Date;

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
@Table(name = "billing_statements")
public class BillingStatement extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "statement_id")
    public Long statementId;

    @Column(name = "card_number", nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "billing_cycle_day", nullable = false)
    public Integer billingCycleDay;

    @Column(name = "opening_balance", precision = 19, scale = 2)
    public BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 19, scale = 2)
    public BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "minimum_payment", precision = 19, scale = 2)
    public BigDecimal minimumPayment = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    public Date dueDate;

    @Column(precision = 19, scale = 2)
    public BigDecimal fees = BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    public BigDecimal interest = BigDecimal.ZERO;

    @Column(name = "statement_date", nullable = false)
    public Date statementDate;

    @Column(length = 20)
    public String status = "OPEN";

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BillingStatement))
            return false;
        return statementId != null && statementId.equals(((BillingStatement) o).statementId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
