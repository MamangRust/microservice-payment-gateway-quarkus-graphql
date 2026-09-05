package com.sanedge.withdraw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawMonthStatusSuccess {
    private String year;
    private String month;
    private Integer totalSuccess;
    private Long totalAmount;
}
