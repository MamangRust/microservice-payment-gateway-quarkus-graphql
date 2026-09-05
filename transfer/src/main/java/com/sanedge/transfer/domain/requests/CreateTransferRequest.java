package com.sanedge.transfer.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateTransferRequest {
    @NotBlank(message = "Transfer from wajib diisi")
    private String transferFrom;

    @NotBlank(message = "Transfer to wajib diisi")
    private String transferTo;

    @Min(value = 50000, message = "Minimal transfer 50000")
    private Long transferAmount;

    @Size(max = 64, message = "Idempotency key maksimal 64 karakter")
    private String idempotencyKey;
}