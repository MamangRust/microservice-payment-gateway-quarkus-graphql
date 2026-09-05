package com.sanedge.topup.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopupYearMethod {
    private String year;
    private String topupMethod;
    private Integer totalTopups;
    private Long totalAmount;
}
