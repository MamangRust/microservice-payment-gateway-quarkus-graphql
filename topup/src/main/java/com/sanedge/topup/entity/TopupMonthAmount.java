package com.sanedge.topup.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopupMonthAmount {
    private String month;
    private Long totalAmount;
}
