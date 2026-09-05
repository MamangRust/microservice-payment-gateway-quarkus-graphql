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

import pb.card.stats.MutinyCardStatsBalanceServiceGrpc;
import pb.card.stats.CardStatsBalance.ApiResponseMonthlyBalance;
import pb.card.stats.CardStatsBalance.ApiResponseYearlyBalance;
import pb.card.stats.CardStatsBalance.CardResponseMonthlyBalance;
import pb.card.stats.CardStatsBalance.CardResponseYearlyBalance;
import pb.card.stats.CardStatsBalance.FindYearBalance;
import pb.card.stats.CardStatsBalance.FindYearBalanceCardNumber;

@GrpcService
@Singleton
public class CardStatsBalanceGrpcHandler extends MutinyCardStatsBalanceServiceGrpc.CardStatsBalanceServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthlyBalance> findMonthlyBalance(FindYearBalance request) {
        return repo.monthlyAmounts("saldo_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyBalance.Builder b = ApiResponseMonthlyBalance.newBuilder().setStatus("success").setMessage("Retrieved monthly balances");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyBalance.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalBalance(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyBalance> findYearlyBalance(FindYearBalance request) {
        return repo.yearlyAmounts("saldo_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyBalance.Builder b = ApiResponseYearlyBalance.newBuilder().setStatus("success").setMessage("Retrieved yearly balances");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyBalance.newBuilder()
                                .setYear(strOf(r, "year")).setTotalBalance(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMonthlyBalance> findMonthlyBalanceByCardNumber(FindYearBalanceCardNumber request) {
        return repo.monthlyAmounts("saldo_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyBalance.Builder b = ApiResponseMonthlyBalance.newBuilder().setStatus("success").setMessage("Retrieved monthly balances by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyBalance.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalBalance(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyBalance> findYearlyBalanceByCardNumber(FindYearBalanceCardNumber request) {
        return repo.yearlyAmounts("saldo_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyBalance.Builder b = ApiResponseYearlyBalance.newBuilder().setStatus("success").setMessage("Retrieved yearly balances by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyBalance.newBuilder()
                                .setYear(strOf(r, "year")).setTotalBalance(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
