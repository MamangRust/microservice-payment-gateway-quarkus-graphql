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

import pb.saldo.stats.MutinySaldoStatsBalanceServiceGrpc;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseMonthSaldoBalances;
import pb.saldo.stats.SaldoStatsBalance.ApiResponseYearSaldoBalances;
import pb.saldo.stats.SaldoStatsBalance.SaldoMonthBalanceResponse;
import pb.saldo.stats.SaldoStatsBalance.SaldoYearBalanceResponse;
import pb.saldo.Saldo.FindYearlySaldo;

@GrpcService
@Singleton
public class SaldoStatsBalanceGrpcHandler extends MutinySaldoStatsBalanceServiceGrpc.SaldoStatsBalanceServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthSaldoBalances> findMonthlySaldoBalances(FindYearlySaldo request) {
        return repo.monthlyAmounts("saldo_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthSaldoBalances.Builder b = ApiResponseMonthSaldoBalances.newBuilder().setStatus("success").setMessage("Retrieved monthly saldo balances");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(SaldoMonthBalanceResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalBalance(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearSaldoBalances> findYearlySaldoBalances(FindYearlySaldo request) {
        return repo.yearlyAmounts("saldo_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearSaldoBalances.Builder b = ApiResponseYearSaldoBalances.newBuilder().setStatus("success").setMessage("Retrieved yearly saldo balances");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(SaldoYearBalanceResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalBalance(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
