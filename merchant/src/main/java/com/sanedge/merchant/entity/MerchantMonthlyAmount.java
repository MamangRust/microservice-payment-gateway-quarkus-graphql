package com.sanedge.merchant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMonthlyAmount {
    private String month;
    private Long totalAmount;
}