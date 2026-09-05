package com.sanedge.withdraw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawMonthStatusFailed {
    private String year;
    private String month;
    private Integer totalFailed;
    private Long totalAmount;
}
