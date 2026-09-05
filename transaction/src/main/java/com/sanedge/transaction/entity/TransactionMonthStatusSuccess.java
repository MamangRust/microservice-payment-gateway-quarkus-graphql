package com.sanedge.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionMonthStatusSuccess {
    private String year;
    private String month;
    private Integer totalSuccess;
    private Long totalAmount;
}
