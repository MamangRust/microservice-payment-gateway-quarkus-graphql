package com.sanedge.merchant.entity;

import java.sql.Timestamp;
import java.util.UUID;

import com.sanedge.common.enums.Status;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transactions", schema = "payment_finance")
public class Transaction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    public Long transactionId;

    @Column(name = "transaction_no", nullable = false, unique = true)
    public UUID transactionNo;

    @Column(name = "card_number", nullable = false)
    public String cardNumber;

    @Column(nullable = false)
    public Integer amount;

    @Column(name = "payment_method", nullable = false)
    public String paymentMethod;

    @Column(name = "merchant_id", nullable = false)
    public Integer merchantId;

    @Column(name = "transaction_time", nullable = false)
    public Timestamp transactionTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Status status = Status.PENDING;
}
