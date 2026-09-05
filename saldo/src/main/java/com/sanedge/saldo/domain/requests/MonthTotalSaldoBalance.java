package com.sanedge.saldo.domain.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.QueryParam;
import lombok.Data;

@Schema(description = "Request untuk total saldo per bulan & tahun")
@Data
public class MonthTotalSaldoBalance {

    @QueryParam("year")
    @Min(value = 1900, message = "Tahun tidak valid")
    @Max(value = 2100, message = "Tahun tidak valid")
    private Long year;

    @QueryParam("month")
    @Min(value = 1, message = "Bulan harus >= 1")
    @Max(value = 12, message = "Bulan harus <= 12")
    private int month;
}