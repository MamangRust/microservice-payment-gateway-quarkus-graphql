package com.sanedge.transaction.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionYearStatusSuccess {
    private String year;
    private Integer totalSuccess;
    private Long totalAmount;
}
