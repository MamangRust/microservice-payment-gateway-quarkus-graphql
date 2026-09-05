package com.sanedge.saldo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaldoYearTotalBalance {
    private String year;
    private Long totalBalance;
}