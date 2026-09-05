package com.sanedge.card.domain.requests;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@RegisterForReflection
public class ReverseTransactionRequest {

    @NotNull(message = "Transaction ID wajib diisi")
    private Long authTxnId;

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @NotNull(message = "Amount wajib diisi")
    private BigDecimal amount;

    @NotBlank(message = "Idempotency key wajib diisi")
    private String idempotencyKey;
}
