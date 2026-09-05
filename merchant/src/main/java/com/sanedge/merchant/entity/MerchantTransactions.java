package com.sanedge.merchant.entity;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantTransactions {
    private Integer transactionId;
    private String cardNumber;
    private Integer amount;
    private String paymentMethod;
    private Integer merchantId;
    private String merchantName;
    private Timestamp transactionTime;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;
}
