package com.sanedge.saldo.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk update saldo (top up)")
public class UpdateSaldoBalance {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 50000, message = "Minimal saldo 50.000")
    private Long totalBalance;

    /** Optional atomic mutation. When present, totalBalance is ignored. */
    private Long deltaBalance;

    private Long minimumBalance = 0L;

    private String operationKey;

    private Long withdrawAmount;

    private java.time.LocalDateTime withdrawTime;
}