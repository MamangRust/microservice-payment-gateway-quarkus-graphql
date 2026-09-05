package com.sanedge.withdraw.domain.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWithdrawRequest {
    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 50000, message = "Minimal withdraw 50000")
    private Long withdrawAmount;

    private LocalDateTime withdrawTime;

    @Size(max = 64, message = "Idempotency key maksimal 64 karakter")
    private String idempotencyKey;
}