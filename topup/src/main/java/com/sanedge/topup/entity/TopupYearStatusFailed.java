package com.sanedge.topup.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopupYearStatusFailed {
    private String year;
    private Integer totalFailed;
    private Long totalAmount;
}
