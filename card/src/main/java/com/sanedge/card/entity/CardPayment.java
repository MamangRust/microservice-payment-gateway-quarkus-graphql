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
@Table(name = "card_payments")
public class CardPayment extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    public Long paymentId;

    @Column(name = "card_number", nullable = false, length = 16)
    public String cardNumber;

    @Column(name = "statement_id")
    public Long statementId;

    @Column(nullable = false, precision = 19, scale = 2)
    public BigDecimal amount;

    @Column(name = "payment_channel", nullable = false, length = 20)
    public String paymentChannel;

    @Column(name = "reference_id", length = 64)
    public String referenceId;

    @Column(length = 20)
    public String status = "PENDING";

    @Column(name = "paid_at")
    public Instant paidAt;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof CardPayment))
            return false;
        return paymentId != null && paymentId.equals(((CardPayment) o).paymentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
