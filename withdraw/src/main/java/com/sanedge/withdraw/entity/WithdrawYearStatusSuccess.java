package com.sanedge.withdraw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawYearStatusSuccess {
    private String year;
    private Integer totalSuccess;
    private Long totalAmount;
}
