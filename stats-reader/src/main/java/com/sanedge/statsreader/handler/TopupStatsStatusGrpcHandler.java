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

import pb.topup.stats.MutinyTopupStatsStatusServiceGrpc;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusSuccess;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusSuccess;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupMonthStatusFailed;
import pb.topup.stats.TopupStatsStatus.ApiResponseTopupYearStatusFailed;
import pb.topup.stats.TopupStatsStatus.TopupMonthStatusSuccessResponse;
import pb.topup.stats.TopupStatsStatus.TopupYearStatusSuccessResponse;
import pb.topup.stats.TopupStatsStatus.TopupMonthStatusFailedResponse;
import pb.topup.stats.TopupStatsStatus.TopupYearStatusFailedResponse;
import pb.topup.Topup.FindMonthlyTopupStatus;
import pb.topup.Topup.FindYearTopupStatus;
import pb.topup.Topup.FindMonthlyTopupStatusCardNumber;
import pb.topup.Topup.FindYearTopupStatusCardNumber;

@GrpcService
@Singleton
public class TopupStatsStatusGrpcHandler extends MutinyTopupStatsStatusServiceGrpc.TopupStatsStatusServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;
    private static final String TABLE = "topup_events";

    @Override
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccess(FindMonthlyTopupStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTopupMonthStatusSuccess.Builder b = ApiResponseTopupMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccess(FindYearTopupStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTopupYearStatusSuccess.Builder b = ApiResponseTopupYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailed(FindMonthlyTopupStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTopupMonthStatusFailed.Builder b = ApiResponseTopupMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailed(FindYearTopupStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTopupYearStatusFailed.Builder b = ApiResponseTopupYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusSuccess> findMonthlyTopupStatusSuccessByCardNumber(FindMonthlyTopupStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTopupMonthStatusSuccess.Builder b = ApiResponseTopupMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearStatusSuccess> findYearlyTopupStatusSuccessByCardNumber(FindYearTopupStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTopupYearStatusSuccess.Builder b = ApiResponseTopupYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupMonthStatusFailed> findMonthlyTopupStatusFailedByCardNumber(FindMonthlyTopupStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTopupMonthStatusFailed.Builder b = ApiResponseTopupMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTopupYearStatusFailed> findYearlyTopupStatusFailedByCardNumber(FindYearTopupStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTopupYearStatusFailed.Builder b = ApiResponseTopupYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TopupYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
