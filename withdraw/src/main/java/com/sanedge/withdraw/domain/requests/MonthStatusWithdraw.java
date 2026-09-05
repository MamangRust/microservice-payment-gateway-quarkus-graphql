package com.sanedge.withdraw.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthStatusWithdraw {

    @QueryParam("year")
    @Schema(description = "Tahun transaksi withdraw", example = "2025", minimum = "2000", maximum = "2100")
    @Min(value = 2000, message = "Tahun tidak valid (minimal 2000)")
    @Max(value = 2100, message = "Tahun tidak valid (maksimal 2100)")
    private Long year;

    @QueryParam("month")
    @Schema(description = "Bulan transaksi withdraw (1-12)", example = "9", minimum = "1", maximum = "12")
    @Min(value = 1, message = "Bulan harus antara 1 - 12")
    @Max(value = 12, message = "Bulan harus antara 1 - 12")
    private int month;
}
