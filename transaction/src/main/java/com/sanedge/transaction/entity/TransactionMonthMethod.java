package com.sanedge.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionMonthMethod {
    private String month;
    private String paymentMethod;
    private Integer totalTransactions;
    private Long totalAmount;
}
