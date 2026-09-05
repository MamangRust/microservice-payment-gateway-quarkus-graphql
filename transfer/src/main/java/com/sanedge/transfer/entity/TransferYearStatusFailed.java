package com.sanedge.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferYearStatusFailed {
    private String year;
    private Integer totalFailed;
    private Long totalAmount;
}
