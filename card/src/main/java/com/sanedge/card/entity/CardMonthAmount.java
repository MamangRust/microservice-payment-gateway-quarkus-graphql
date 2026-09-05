package com.sanedge.card.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardMonthAmount {
    private String month;
    private Long totalAmount;
}

