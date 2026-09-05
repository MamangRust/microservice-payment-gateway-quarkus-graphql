package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.SaldoDto.*;
import io.smallrye.mutiny.Uni;

public interface SaldoService {
    Uni<FindAllSaldoResponse> listSaldos(int page, int size, String search);
    Uni<FindByIdSaldoResponse> getSaldo(int id);
    Uni<FindByIdSaldoResponse> findSaldoByCard(String cardNumber);
    Uni<FindAllSaldoResponse> findActiveSaldos(int page, int size, String search);
    Uni<FindAllSaldoResponse> findTrashedSaldos(int page, int size, String search);
    Uni<CreateSaldoResponse> createSaldo(CreateSaldoRequest body);
    Uni<UpdateSaldoResponse> updateSaldo(int id, UpdateSaldoRequest body);
    Uni<UpdateSaldoResponse> updateSaldoBalance(UpdateSaldoBalanceRequest body);
    Uni<UpdateSaldoResponse> updateSaldoWithdraw(UpdateSaldoWithdrawRequest body);
    Uni<TrashedSaldoResponse> deleteSaldo(int id);
    Uni<TrashedSaldoResponse> restoreSaldo(int id);
    Uni<SimpleStatusMessageResponse> deleteSaldoPermanent(int id);
    Uni<SimpleStatusMessageResponse> restoreAllSaldos();
    Uni<SimpleStatusMessageResponse> deleteAllSaldos();

    Uni<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(int year);
    Uni<ApiResponseYearSaldoBalances> findYearlySaldoBalances(int year);
    Uni<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(int year, int month);
    Uni<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(int year);
}
