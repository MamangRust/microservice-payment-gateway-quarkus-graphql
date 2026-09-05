package com.sanedge.merchant.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantMonthlyPaymentMethod {
    private String month;
    private String paymentMethod;
    private Long totalAmount;
}
