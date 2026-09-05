package com.sanedge.saldo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaldoYearBalance {
    private String year;
    private Long totalBalance;
}
