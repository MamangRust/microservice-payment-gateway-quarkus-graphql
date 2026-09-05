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

import pb.withdraw.stats.MutinyWithdrawStatsStatusServiceGrpc;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusSuccess;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusSuccess;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawMonthStatusFailed;
import pb.withdraw.stats.WithdrawStatsStatus.ApiResponseWithdrawYearStatusFailed;
import pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusSuccessResponse;
import pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusSuccessResponse;
import pb.withdraw.stats.WithdrawStatsStatus.WithdrawMonthStatusFailedResponse;
import pb.withdraw.stats.WithdrawStatsStatus.WithdrawYearStatusFailedResponse;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatus;
import pb.withdraw.Withdraw.FindYearWithdrawStatus;
import pb.withdraw.Withdraw.FindMonthlyWithdrawStatusCardNumber;
import pb.withdraw.Withdraw.FindYearWithdrawStatusCardNumber;

@GrpcService
@Singleton
public class WithdrawStatsStatusGrpcHandler extends MutinyWithdrawStatsStatusServiceGrpc.WithdrawStatsStatusServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;
    private static final String TABLE = "withdraw_events";

    @Override
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccess(FindMonthlyWithdrawStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseWithdrawMonthStatusSuccess.Builder b = ApiResponseWithdrawMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccess(FindYearWithdrawStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseWithdrawYearStatusSuccess.Builder b = ApiResponseWithdrawYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailed(FindMonthlyWithdrawStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseWithdrawMonthStatusFailed.Builder b = ApiResponseWithdrawMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailed(FindYearWithdrawStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseWithdrawYearStatusFailed.Builder b = ApiResponseWithdrawYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusSuccess> findMonthlyWithdrawStatusSuccessCardNumber(FindMonthlyWithdrawStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseWithdrawMonthStatusSuccess.Builder b = ApiResponseWithdrawMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusSuccess> findYearlyWithdrawStatusSuccessCardNumber(FindYearWithdrawStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseWithdrawYearStatusSuccess.Builder b = ApiResponseWithdrawYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawMonthStatusFailed> findMonthlyWithdrawStatusFailedCardNumber(FindMonthlyWithdrawStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseWithdrawMonthStatusFailed.Builder b = ApiResponseWithdrawMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseWithdrawYearStatusFailed> findYearlyWithdrawStatusFailedCardNumber(FindYearWithdrawStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseWithdrawYearStatusFailed.Builder b = ApiResponseWithdrawYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(WithdrawYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
