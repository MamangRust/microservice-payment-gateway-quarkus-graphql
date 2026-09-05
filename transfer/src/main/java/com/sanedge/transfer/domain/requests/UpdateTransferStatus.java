package com.sanedge.transfer.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTransferStatus {
    @Min(value = 1, message = "Transfer ID minimal 1")
    private int transferId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}