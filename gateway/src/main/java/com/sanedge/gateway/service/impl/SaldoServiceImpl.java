package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.SaldoDto.*;
import com.sanedge.gateway.service.SaldoService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SaldoServiceImpl implements SaldoService {

    private static final Logger LOG = Logger.getLogger(SaldoServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("saldo")
    pb.saldo.MutinySaldoQueryServiceGrpc.MutinySaldoQueryServiceStub saldoQueryService;

    @GrpcClient("saldo")
    pb.saldo.MutinySaldoCommandServiceGrpc.MutinySaldoCommandServiceStub saldoCommandService;

    @GrpcClient("statsreader")
    pb.saldo.stats.MutinySaldoStatsBalanceServiceGrpc.MutinySaldoStatsBalanceServiceStub saldoStatsBalanceService;

    @GrpcClient("statsreader")
    pb.saldo.stats.MutinySaldoStatsTotalBalanceGrpc.MutinySaldoStatsTotalBalanceStub saldoStatsTotalBalanceService;

    @Override
    public Uni<FindAllSaldoResponse> listSaldos(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("saldo.listSaldos", () -> saldoQueryService.findAllSaldo(
                pb.saldo.Saldo.FindAllSaldoRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list saldos: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdSaldoResponse> getSaldo(int id) {
        return telemetryHelper.traceAndMetric("saldo.getSaldo", () -> saldoQueryService.findByIdSaldo(
                pb.saldo.Saldo.FindByIdSaldoRequest.newBuilder()
                        .setSaldoId(id)
                        .build())
                .map(FindByIdSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdSaldoResponse> findSaldoByCard(String cardNumber) {
        return telemetryHelper.traceAndMetric("saldo.findSaldoByCard", () -> saldoQueryService.findByCardNumber(
                pb.card.Card.FindByCardNumberRequest.newBuilder()
                        .setCardNumber(cardNumber == null ? "" : cardNumber)
                        .build())
                .map(FindByIdSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find saldo by card: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllSaldoResponse> findActiveSaldos(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("saldo.findActiveSaldos", () -> saldoQueryService.findByActive(
                pb.saldo.Saldo.FindAllSaldoRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active saldos: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllSaldoResponse> findTrashedSaldos(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("saldo.findTrashedSaldos", () -> saldoQueryService.findByTrashed(
                pb.saldo.Saldo.FindAllSaldoRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed saldos: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateSaldoResponse> createSaldo(CreateSaldoRequest body) {
        return telemetryHelper.traceAndMetric("saldo.createSaldo", () -> saldoCommandService.createSaldo(
                pb.saldo.SaldoCommand.CreateSaldoRequest.newBuilder()
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTotalBalance(body.totalBalance())
                        .build())
                .map(CreateSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateSaldoResponse> updateSaldo(int id, UpdateSaldoRequest body) {
        return telemetryHelper.traceAndMetric("saldo.updateSaldo", () -> saldoCommandService.updateSaldo(
                pb.saldo.SaldoCommand.UpdateSaldoRequest.newBuilder()
                        .setSaldoId(id)
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTotalBalance(body.totalBalance())
                        .build())
                .map(UpdateSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateSaldoResponse> updateSaldoBalance(UpdateSaldoBalanceRequest body) {
        return telemetryHelper.traceAndMetric("saldo.updateSaldoBalance", () -> saldoCommandService.updateSaldoBalance(
                pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTotalBalance(body.totalBalance())
                        .build())
                .map(UpdateSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update saldo balance: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateSaldoResponse> updateSaldoWithdraw(UpdateSaldoWithdrawRequest body) {
        return telemetryHelper.traceAndMetric("saldo.updateSaldoWithdraw", () -> saldoCommandService.updateSaldoWithdraw(
                pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest.newBuilder()
                        .setCardNumber(body.cardNumber() == null ? "" : body.cardNumber())
                        .setTotalBalance(body.totalBalance())
                        .setWithdrawTime(body.withdrawTime() == null ? "" : body.withdrawTime())
                        .setWithdrawAmount(body.withdrawAmount())
                        .build())
                .map(UpdateSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update saldo withdraw: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedSaldoResponse> deleteSaldo(int id) {
        return telemetryHelper.traceAndMetric("saldo.deleteSaldo", () -> saldoCommandService.trashedSaldo(
                pb.saldo.Saldo.FindByIdSaldoRequest.newBuilder()
                        .setSaldoId(id)
                        .build())
                .map(TrashedSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedSaldoResponse> restoreSaldo(int id) {
        return telemetryHelper.traceAndMetric("saldo.restoreSaldo", () -> saldoCommandService.restoreSaldo(
                pb.saldo.Saldo.FindByIdSaldoRequest.newBuilder()
                        .setSaldoId(id)
                        .build())
                .map(TrashedSaldoResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteSaldoPermanent(int id) {
        return telemetryHelper.traceAndMetric("saldo.deleteSaldoPermanent", () -> saldoCommandService.deleteSaldoPermanent(
                pb.saldo.Saldo.FindByIdSaldoRequest.newBuilder()
                        .setSaldoId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete saldo: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllSaldos() {
        return telemetryHelper.traceAndMetric("saldo.restoreAllSaldos", () -> saldoCommandService.restoreAllSaldo(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all saldos: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllSaldos() {
        return telemetryHelper.traceAndMetric("saldo.deleteAllSaldos", () -> saldoCommandService.deleteAllSaldoPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all saldos: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(int year) {
        return telemetryHelper.traceAndMetric("saldo.findMonthlySaldoBalances", () -> saldoStatsBalanceService.findMonthlySaldoBalances(
                pb.saldo.Saldo.FindYearlySaldo.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseMonthSaldoBalances::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly saldo balance stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearSaldoBalances> findYearlySaldoBalances(int year) {
        return telemetryHelper.traceAndMetric("saldo.findYearlySaldoBalances", () -> saldoStatsBalanceService.findYearlySaldoBalances(
                pb.saldo.Saldo.FindYearlySaldo.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseYearSaldoBalances::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly saldo balance stats: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(int year, int month) {
        return telemetryHelper.traceAndMetric("saldo.findMonthlyTotalSaldoBalance", () -> saldoStatsTotalBalanceService.findMonthlyTotalSaldoBalance(
                pb.saldo.Saldo.FindMonthlySaldoTotalBalance.newBuilder()
                        .setYear(year)
                        .setMonth(month)
                        .build())
                .map(ApiResponseMonthTotalSaldo::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get monthly total saldo balance: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(int year) {
        return telemetryHelper.traceAndMetric("saldo.findYearTotalSaldoBalance", () -> saldoStatsTotalBalanceService.findYearTotalSaldoBalance(
                pb.saldo.Saldo.FindYearlySaldo.newBuilder()
                        .setYear(year)
                        .build())
                .map(ApiResponseYearTotalSaldo::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get yearly total saldo balance: " + throwable.getMessage(), throwable)));
    }
}
