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

import pb.transfer.stats.MutinyTransferStatsStatusServiceGrpc;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusSuccess;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusSuccess;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferMonthStatusFailed;
import pb.transfer.stats.TransferStatsStatus.ApiResponseTransferYearStatusFailed;
import pb.transfer.stats.TransferStatsStatus.TransferMonthStatusSuccessResponse;
import pb.transfer.stats.TransferStatsStatus.TransferYearStatusSuccessResponse;
import pb.transfer.stats.TransferStatsStatus.TransferMonthStatusFailedResponse;
import pb.transfer.stats.TransferStatsStatus.TransferYearStatusFailedResponse;
import pb.transfer.Transfer.FindMonthlyTransferStatus;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.Transfer.FindMonthlyTransferStatusCardNumber;
import pb.transfer.Transfer.FindYearTransferStatusCardNumber;

@GrpcService
@Singleton
public class TransferStatsStatusGrpcHandler extends MutinyTransferStatsStatusServiceGrpc.TransferStatsStatusServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;
    private static final String TABLE = "transfer_events";

    @Override
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccess(FindMonthlyTransferStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransferMonthStatusSuccess.Builder b = ApiResponseTransferMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccess(FindYearTransferStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransferYearStatusSuccess.Builder b = ApiResponseTransferYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailed(FindMonthlyTransferStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransferMonthStatusFailed.Builder b = ApiResponseTransferMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailed(FindYearTransferStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransferYearStatusFailed.Builder b = ApiResponseTransferYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusSuccess> findMonthlyTransferStatusSuccessByCardNumber(FindMonthlyTransferStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransferMonthStatusSuccess.Builder b = ApiResponseTransferMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearStatusSuccess> findYearlyTransferStatusSuccessByCardNumber(FindYearTransferStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransferYearStatusSuccess.Builder b = ApiResponseTransferYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferMonthStatusFailed> findMonthlyTransferStatusFailedByCardNumber(FindMonthlyTransferStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransferMonthStatusFailed.Builder b = ApiResponseTransferMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearStatusFailed> findYearlyTransferStatusFailedByCardNumber(FindYearTransferStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransferYearStatusFailed.Builder b = ApiResponseTransferYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
