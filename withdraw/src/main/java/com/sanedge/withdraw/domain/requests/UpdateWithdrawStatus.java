package com.sanedge.withdraw.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWithdrawStatus {
    @Min(value = 1, message = "Withdraw ID wajib diisi")
    private int withdrawId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}