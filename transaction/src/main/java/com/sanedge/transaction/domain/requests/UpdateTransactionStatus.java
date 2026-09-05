package com.sanedge.transaction.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTransactionStatus {

    @Min(value = 1, message = "Transaction ID minimal 1")
    private Long transactionId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}