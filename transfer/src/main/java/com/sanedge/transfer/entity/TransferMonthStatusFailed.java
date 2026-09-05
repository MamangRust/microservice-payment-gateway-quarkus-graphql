package com.sanedge.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferMonthStatusFailed {
    private String year;
    private String month;
    private Integer totalFailed;
    private Long totalAmount;
}