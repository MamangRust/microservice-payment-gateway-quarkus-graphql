package com.sanedge.withdraw.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawYearStatusFailed {
    private String year;
    private Integer totalFailed;
    private Long totalAmount;
}
