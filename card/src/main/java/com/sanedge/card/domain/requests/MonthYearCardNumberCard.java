package com.sanedge.card.domain.requests;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@RegisterForReflection
@AllArgsConstructor
@NoArgsConstructor
public class MonthYearCardNumberCard {

    @NotBlank(message = "Card number wajib diisi")
    private String cardNumber;

    @Min(value = 2000, message = "Tahun harus antara 2000 dan 2100")
    @Max(value = 2100, message = "Tahun harus antara 2000 dan 2100")
    private Long year;
}
