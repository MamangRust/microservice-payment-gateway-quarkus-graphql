package com.sanedge.merchant.domain.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateMerchantDocumentRequest {
    @Min(value = 1, message = "merchantId minimal 1")
    private Long merchantId;

    @NotBlank(message = "Document type wajib diisi")
    private String documentType;

    @NotBlank(message = "Document url wajib diisi")
    private String documentUrl;
}
