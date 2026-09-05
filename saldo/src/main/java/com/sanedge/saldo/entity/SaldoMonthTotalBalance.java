package com.sanedge.saldo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaldoMonthTotalBalance {
    private String year;
    private String month;
    private Long totalBalance;
}