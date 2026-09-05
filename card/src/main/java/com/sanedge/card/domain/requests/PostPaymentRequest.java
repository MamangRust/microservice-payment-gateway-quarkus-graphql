package com.sanedge.card.domain.requests;

import java.math.BigDecimal;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@RegisterForReflection
public class PostPaymentRequest {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    private Long statementId;

    @NotNull(message = "Amount wajib diisi")
    private BigDecimal amount;

    @NotBlank(message = "Payment channel wajib diisi")
    private String paymentChannel;

    private String referenceId;
}
