package com.sanedge.statsreader.handler;

import static com.sanedge.statsreader.handler.StatsRow.intOf;
import static com.sanedge.statsreader.handler.StatsRow.longOf;
import static com.sanedge.statsreader.handler.StatsRow.strOf;

import io.grpc.Status;
import com.sanedge.statsreader.repository.ClickHouseStatsRepository;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Singleton;

import pb.saldo.stats.MutinySaldoStatsTotalBalanceGrpc;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseMonthTotalSaldo;
import pb.saldo.stats.SaldoStatsTotal.ApiResponseYearTotalSaldo;
import pb.saldo.stats.SaldoStatsTotal.SaldoMonthTotalBalanceResponse;
import pb.saldo.stats.SaldoStatsTotal.SaldoYearTotalBalanceResponse;
import pb.saldo.Saldo.FindMonthlySaldoTotalBalance;
import pb.saldo.Saldo.FindYearlySaldo;

@GrpcService
@Singleton
public class SaldoStatsTotalBalanceGrpcHandler extends MutinySaldoStatsTotalBalanceGrpc.SaldoStatsTotalBalanceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthTotalSaldo> findMonthlyTotalSaldoBalance(FindMonthlySaldoTotalBalance request) {
        return repo.monthlyTotalSaldo(request.getYear())
                .map(rows -> {
                    ApiResponseMonthTotalSaldo.Builder b = ApiResponseMonthTotalSaldo.newBuilder().setStatus("success").setMessage("Retrieved monthly total saldo balance");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(SaldoMonthTotalBalanceResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalBalance(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearTotalSaldo> findYearTotalSaldoBalance(FindYearlySaldo request) {
        return repo.yearlyTotalSaldo(request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearTotalSaldo.Builder b = ApiResponseYearTotalSaldo.newBuilder().setStatus("success").setMessage("Retrieved yearly total saldo balance");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(SaldoYearTotalBalanceResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalBalance(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
