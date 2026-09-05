package com.sanedge.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferMonthStatusSuccess {
    private String year;
    private String month;
    private Integer totalSuccess;
    private Long totalAmount;
}
