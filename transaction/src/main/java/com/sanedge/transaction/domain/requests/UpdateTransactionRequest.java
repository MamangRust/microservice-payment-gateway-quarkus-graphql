package com.sanedge.transaction.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Schema(description = "Request untuk update transaksi")
public class UpdateTransactionRequest {

    @Min(value = 1, message = "Transaction ID wajib diisi")
    private Long transactionId;

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 50000, message = "Minimal transaksi 50.000")
    private Long amount;

    @NotBlank(message = "Payment method wajib diisi")
    private String paymentMethod;

    @Min(value = 1, message = "Merchant ID minimal 1")
    private Long merchantId;

    private LocalDateTime transactionTime;
}