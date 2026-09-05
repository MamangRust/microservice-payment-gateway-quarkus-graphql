package com.sanedge.topup.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request untuk membuat topup baru")
public class CreateTopupRequest {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 50000, message = "Minimal topup 50.000")
    private Long topupAmount;

    @NotBlank(message = "Topup method wajib diisi")
    private String topupMethod;

    @Size(max = 64, message = "Idempotency key maksimal 64 karakter")
    private String idempotencyKey;
}