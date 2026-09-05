package com.sanedge.card.domain.requests;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@RegisterForReflection
public class AuthorizeCardRequest {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @NotNull(message = "Merchant ID wajib diisi")
    private Integer merchantId;

    @NotNull(message = "Amount wajib diisi")
    private BigDecimal amount;

    private String currency;

    private String posEntryMode;

    private String mcc;

    @NotBlank(message = "Idempotency key wajib diisi")
    private String idempotencyKey;
}
