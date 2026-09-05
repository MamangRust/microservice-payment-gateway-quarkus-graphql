package com.sanedge.merchant.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update merchant")
public class UpdateMerchantRequest {
    private Long merchantId;

    @NotBlank(message = "Nama merchant wajib diisi")
    private String name;

    @Min(value = 1, message = "User ID minimal 1")
    private Long userId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}