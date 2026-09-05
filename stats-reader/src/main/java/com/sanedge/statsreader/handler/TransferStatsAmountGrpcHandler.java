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

import pb.transfer.stats.MutinyTransferStatsAmountServiceGrpc;
import pb.transfer.stats.TransferStatsAmount.ApiResponseTransferMonthAmount;
import pb.transfer.stats.TransferStatsAmount.ApiResponseTransferYearAmount;
import pb.transfer.stats.TransferStatsAmount.TransferMonthAmountResponse;
import pb.transfer.stats.TransferStatsAmount.TransferYearAmountResponse;
import pb.transfer.Transfer.FindYearTransferStatus;
import pb.transfer.Transfer.FindByCardNumberTransferRequest;

@GrpcService
@Singleton
public class TransferStatsAmountGrpcHandler extends MutinyTransferStatsAmountServiceGrpc.TransferStatsAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyTransferAmounts(FindYearTransferStatus request) {
        return repo.monthlyAmounts("transfer_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseTransferMonthAmount.Builder b = ApiResponseTransferMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyTransferAmounts(FindYearTransferStatus request) {
        return repo.yearlyAmounts("transfer_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTransferYearAmount.Builder b = ApiResponseTransferYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsBySenderCardNumber(FindByCardNumberTransferRequest request) {
        return repo.monthlyTransferByCard("source_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTransferMonthAmount.Builder b = ApiResponseTransferMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer amounts by sender card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyTransferAmountsBySenderCardNumber(FindByCardNumberTransferRequest request) {
        return repo.yearlyTransferByCard("source_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTransferYearAmount.Builder b = ApiResponseTransferYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer amounts by sender card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferMonthAmount> findMonthlyTransferAmountsByReceiverCardNumber(FindByCardNumberTransferRequest request) {
        return repo.monthlyTransferByCard("destination_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTransferMonthAmount.Builder b = ApiResponseTransferMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer amounts by receiver card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransferYearAmount> findYearlyTransferAmountsByReceiverCardNumber(FindByCardNumberTransferRequest request) {
        return repo.yearlyTransferByCard("destination_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTransferYearAmount.Builder b = ApiResponseTransferYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer amounts by receiver card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransferYearAmountResponse.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
