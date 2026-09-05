package com.sanedge.saldo.service;

import java.util.List;

import com.sanedge.saldo.domain.requests.FindAllSaldos;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface SaldoQueryService {
    Uni<ApiResponsePagination<List<SaldoResponse>>> findAll(FindAllSaldos req);

    Uni<ApiResponsePagination<List<SaldoResponseDeleteAt>>> findActive(FindAllSaldos req);

    Uni<ApiResponsePagination<List<SaldoResponseDeleteAt>>> findTrashed(FindAllSaldos req);

    Uni<ApiResponse<SaldoResponse>> findByCard(String cardNumber);

    Uni<ApiResponse<SaldoResponse>> findById(Long id);
}
