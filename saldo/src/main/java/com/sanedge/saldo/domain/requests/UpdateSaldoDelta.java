package com.sanedge.saldo.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request untuk mutasi saldo secara atomic menggunakan delta")
public class UpdateSaldoDelta {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    private Long deltaBalance;

    private Long minimumBalance = 0L;

    private Long withdrawAmount;

    private java.time.LocalDateTime withdrawTime;
}
