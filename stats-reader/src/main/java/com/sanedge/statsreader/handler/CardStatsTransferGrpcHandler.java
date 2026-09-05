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

import pb.card.stats.MutinyCardStatsTransferServiceGrpc;
import pb.card.Card.ApiResponseMonthlyAmount;
import pb.card.Card.ApiResponseYearlyAmount;
import pb.card.Card.CardResponseMonthlyAmount;
import pb.card.Card.CardResponseYearlyAmount;
import pb.card.Card.FindYearAmount;
import pb.card.Card.FindYearAmountCardNumber;

@GrpcService
@Singleton
public class CardStatsTransferGrpcHandler extends MutinyCardStatsTransferServiceGrpc.CardStatsTransferServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmount(FindYearAmount request) {
        return repo.monthlyAmounts("transfer_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer sender amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferSenderAmount(FindYearAmount request) {
        return repo.yearlyAmounts("transfer_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer sender amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmount(FindYearAmount request) {
        return repo.monthlyAmounts("transfer_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer receiver amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferReceiverAmount(FindYearAmount request) {
        return repo.yearlyAmounts("transfer_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer receiver amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferSenderAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.monthlyTransferByCard("source_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer sender amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferSenderAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.yearlyTransferByCard("source_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer sender amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTransferReceiverAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.monthlyTransferByCard("destination_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transfer receiver amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseYearlyAmount> findYearlyTransferReceiverAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.yearlyTransferByCard("destination_card", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transfer receiver amounts by card");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(CardResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
