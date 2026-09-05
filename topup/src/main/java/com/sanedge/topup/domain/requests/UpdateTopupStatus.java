package com.sanedge.topup.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update status topup")
public class UpdateTopupStatus {

    @Min(value = 1, message = "Topup ID wajib diisi")
    private Long topupId;

    @NotBlank(message = "Status wajib diisi")
    private String status;
}