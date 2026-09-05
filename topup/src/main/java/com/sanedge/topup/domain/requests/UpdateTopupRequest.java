package com.sanedge.topup.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update topup")
public class UpdateTopupRequest {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 1, message = "Topup ID wajib diisi")
    private Long topupId;

    @Min(value = 50000, message = "Minimal topup 50.000")
    private Long topupAmount;

    @NotBlank(message = "Topup method wajib diisi")
    private String topupMethod;
}