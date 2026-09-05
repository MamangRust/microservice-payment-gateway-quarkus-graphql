package com.sanedge.withdraw.domain.requests;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWithdrawRequest {
    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    private Long withdrawId;

    @Min(value = 50000, message = "Minimal withdraw 50000")
    private Long withdrawAmount;

    private LocalDateTime withdrawTime;
}
