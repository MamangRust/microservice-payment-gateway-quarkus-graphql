package com.sanedge.saldo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaldoMonthBalance {
    private String month;
    private Long totalBalance;
}
