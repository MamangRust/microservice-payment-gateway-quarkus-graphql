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

import pb.withdraw.stats.MutinyWithdrawStatsAmountServiceGrpc;
import pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawMonthAmount;
import pb.withdraw.stats.WithdrawStatsAmount.ApiResponseWithdrawYearAmount;
import pb.withdraw.stats.WithdrawStatsAmount.WithdrawMonthlyAmountResponse;
import pb.withdraw.stats.WithdrawStatsAmount.WithdrawYearlyAmountResponse;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.Withdraw.FindYearWithdrawCardNumber;

@GrpcService
@Singleton
public class WithdrawStatsAmountGrpcHandler extends MutinyWithdrawStatsAmountServiceGrpc.WithdrawStatsAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyWithdraws(FindYearWithdrawStatus request) {
        return repo.monthlyAmounts("withdraw_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseWithdrawMonthAmount.Builder b = ApiResponseWithdrawMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly withdraw amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthlyAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearAmount> findYearlyWithdraws(FindYearWithdrawStatus request) {
        return repo.yearlyAmounts("withdraw_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseWithdrawYearAmount.Builder b = ApiResponseWithdrawYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly withdraw amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawMonthAmount> findMonthlyWithdrawsByCardNumber(FindYearWithdrawCardNumber request) {
        return repo.monthlyAmounts("withdraw_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseWithdrawMonthAmount.Builder b = ApiResponseWithdrawMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly withdraw amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthlyAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearAmount> findYearlyWithdrawsByCardNumber(FindYearWithdrawCardNumber request) {
        return repo.yearlyAmounts("withdraw_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseWithdrawYearAmount.Builder b = ApiResponseWithdrawYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly withdraw amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
