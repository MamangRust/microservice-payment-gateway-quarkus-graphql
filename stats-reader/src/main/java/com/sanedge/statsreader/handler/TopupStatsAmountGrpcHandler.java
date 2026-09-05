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

import pb.topup.stats.MutinyTopupStatsAmountServiceGrpc;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupMonthAmount;
import pb.topup.stats.TopupStatsAmount.ApiResponseTopupYearAmount;
import pb.topup.stats.TopupStatsAmount.TopupMonthAmountResponse;
import pb.topup.stats.TopupStatsAmount.TopupYearlyAmountResponse;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindYearTopupCardNumber;

@GrpcService
@Singleton
public class TopupStatsAmountGrpcHandler extends MutinyTopupStatsAmountServiceGrpc.TopupStatsAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseTopupMonthAmount> findMonthlyTopupAmounts(FindYearTopupStatus request) {
        return repo.monthlyAmounts("topup_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseTopupMonthAmount.Builder b = ApiResponseTopupMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly topup amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearAmount> findYearlyTopupAmounts(FindYearTopupStatus request) {
        return repo.yearlyAmounts("topup_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupYearAmount.Builder b = ApiResponseTopupYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly topup amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupMonthAmount> findMonthlyTopupAmountsByCardNumber(FindYearTopupCardNumber request) {
        return repo.monthlyAmounts("topup_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupMonthAmount.Builder b = ApiResponseTopupMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly topup amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearAmount> findYearlyTopupAmountsByCardNumber(FindYearTopupCardNumber request) {
        return repo.yearlyAmounts("topup_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupYearAmount.Builder b = ApiResponseTopupYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly topup amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
