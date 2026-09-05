package com.sanedge.saldo.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update saldo")
public class UpdateSaldoRequest {

    @Min(value = 1, message = "Saldo ID minimal 1")
    private Long saldoId;

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 1, message = "Total balance wajib diisi")
    private Long totalBalance;
}