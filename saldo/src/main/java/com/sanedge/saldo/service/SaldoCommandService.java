package com.sanedge.saldo.service;

import com.sanedge.saldo.domain.requests.CreateSaldoRequest;
import com.sanedge.saldo.domain.requests.UpdateSaldoRequest;
import com.sanedge.saldo.domain.requests.UpdateSaldoBalance;
import com.sanedge.saldo.domain.requests.UpdateSaldoWithdraw;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface SaldoCommandService {
    Uni<ApiResponse<SaldoResponse>> create(CreateSaldoRequest request);

    Uni<ApiResponse<SaldoResponse>> update(UpdateSaldoRequest request);

    Uni<ApiResponse<SaldoResponse>> updateSaldoBalance(UpdateSaldoBalance request);

    Uni<ApiResponse<SaldoResponse>> updateSaldoWithdraw(UpdateSaldoWithdraw request);

    Uni<ApiResponse<SaldoResponseDeleteAt>> trash(Long id);

    Uni<ApiResponse<SaldoResponseDeleteAt>> restore(Long id);

    Uni<ApiResponse<Boolean>> delete(Long id);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAll();
}

