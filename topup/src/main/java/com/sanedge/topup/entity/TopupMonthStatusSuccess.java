package com.sanedge.topup.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopupMonthStatusSuccess {
    private String year;
    private String month;
    private Integer totalSuccess;
    private Long totalAmount;
}
