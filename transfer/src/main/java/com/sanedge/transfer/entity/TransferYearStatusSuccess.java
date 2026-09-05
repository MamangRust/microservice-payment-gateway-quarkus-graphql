package com.sanedge.transfer.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferYearStatusSuccess {
    private String year;
    private Integer totalSuccess;
    private Long totalAmount;
}
