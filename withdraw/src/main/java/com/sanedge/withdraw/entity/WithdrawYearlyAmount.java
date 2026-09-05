package com.sanedge.withdraw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawYearlyAmount {
    private String year;
    private Long totalAmount;
}
