package com.sanedge.topup.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Request untuk update jumlah topup")
public class UpdateTopupAmount {

    @Min(value = 1, message = "Topup ID wajib diisi")
    private Long topupId;

    @Min(value = 50000, message = "Minimal topup 50.000")
    private Long topupAmount;
}