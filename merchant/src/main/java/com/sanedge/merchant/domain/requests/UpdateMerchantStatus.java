package com.sanedge.merchant.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update status merchant")
public class UpdateMerchantStatus {
    @Min(value = 1, message = "merchant_id minimal 1")
    private Long merchantId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}