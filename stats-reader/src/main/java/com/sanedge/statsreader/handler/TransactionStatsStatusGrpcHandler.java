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

import pb.transaction.stats.MutinyTransactionStatsStatusServiceGrpc;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusSuccess;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusSuccess;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionMonthStatusFailed;
import pb.transaction.stats.TransactionStatsStatus.ApiResponseTransactionYearStatusFailed;
import pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusSuccessResponse;
import pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusSuccessResponse;
import pb.transaction.stats.TransactionStatsStatus.TransactionMonthStatusFailedResponse;
import pb.transaction.stats.TransactionStatsStatus.TransactionYearStatusFailedResponse;
import pb.transaction.Transaction.FindMonthlyTransactionStatus;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindMonthlyTransactionStatusCardNumber;
import pb.transaction.Transaction.FindYearTransactionStatusCardNumber;

@GrpcService
@Singleton
public class TransactionStatsStatusGrpcHandler extends MutinyTransactionStatsStatusServiceGrpc.TransactionStatsStatusServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;
    private static final String TABLE = "transaction_events";

    @Override
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccess(FindMonthlyTransactionStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransactionMonthStatusSuccess.Builder b = ApiResponseTransactionMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccess(FindYearTransactionStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransactionYearStatusSuccess.Builder b = ApiResponseTransactionYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailed(FindMonthlyTransactionStatus request) {
        return repo.monthlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransactionMonthStatusFailed.Builder b = ApiResponseTransactionMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailed(FindYearTransactionStatus request) {
        return repo.yearlyStatusStats(TABLE, null, null, request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransactionYearStatusFailed.Builder b = ApiResponseTransactionYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccessByCardNumber(FindMonthlyTransactionStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransactionMonthStatusSuccess.Builder b = ApiResponseTransactionMonthStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved monthly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccessByCardNumber(FindYearTransactionStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "success")
                .map(rows -> {
                    ApiResponseTransactionYearStatusSuccess.Builder b = ApiResponseTransactionYearStatusSuccess.newBuilder().setStatus("success").setMessage("Retrieved yearly status success by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearStatusSuccessResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalSuccess(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailedByCardNumber(FindMonthlyTransactionStatusCardNumber request) {
        return repo.monthlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransactionMonthStatusFailed.Builder b = ApiResponseTransactionMonthStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved monthly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year")).setMonth(strOf(r, "month"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailedByCardNumber(FindYearTransactionStatusCardNumber request) {
        return repo.yearlyStatusStats(TABLE, "card_number", request.getCardNumber(), request.getYear(), "failed")
                .map(rows -> {
                    ApiResponseTransactionYearStatusFailed.Builder b = ApiResponseTransactionYearStatusFailed.newBuilder().setStatus("success").setMessage("Retrieved yearly status failed by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearStatusFailedResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setTotalFailed(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
