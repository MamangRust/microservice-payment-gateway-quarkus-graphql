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

import pb.topup.stats.MutinyTopupStatsMethodServiceGrpc;
import pb.topup.stats.TopupStatsMethod.ApiResponseTopupMonthMethod;
import pb.topup.stats.TopupStatsMethod.ApiResponseTopupYearMethod;
import pb.topup.stats.TopupStatsMethod.TopupMonthMethodResponse;
import pb.topup.stats.TopupStatsMethod.TopupYearlyMethodResponse;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindYearTopupCardNumber;

@GrpcService
@Singleton
public class TopupStatsMethodGrpcHandler extends MutinyTopupStatsMethodServiceGrpc.TopupStatsMethodServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseTopupMonthMethod> findMonthlyTopupMethods(FindYearTopupStatus request) {
        return repo.monthlyMethodStats("topup_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseTopupMonthMethod.Builder b = ApiResponseTopupMonthMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly topup methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthMethodResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTopupMethod(strOf(r, "method"))
                                .setTotalTopups(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearMethod> findYearlyTopupMethods(FindYearTopupStatus request) {
        return repo.yearlyMethodStats("topup_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupYearMethod.Builder b = ApiResponseTopupYearMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly topup methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearlyMethodResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTopupMethod(strOf(r, "method"))
                                .setTotalTopups(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupMonthMethod> findMonthlyTopupMethodsByCardNumber(FindYearTopupCardNumber request) {
        return repo.monthlyMethodStats("topup_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupMonthMethod.Builder b = ApiResponseTopupMonthMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly topup methods by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthMethodResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTopupMethod(strOf(r, "method"))
                                .setTotalTopups(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearMethod> findYearlyTopupMethodsByCardNumber(FindYearTopupCardNumber request) {
        return repo.yearlyMethodStats("topup_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTopupYearMethod.Builder b = ApiResponseTopupYearMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly topup methods by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearlyMethodResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTopupMethod(strOf(r, "method"))
                                .setTotalTopups(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
