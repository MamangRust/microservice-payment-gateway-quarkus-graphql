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

import pb.card.stats.MutinyCardStatsWithdrawServiceGrpc;
import pb.card.Card.ApiResponseMonthlyAmount;
import pb.card.Card.ApiResponseYearlyAmount;
import pb.card.Card.CardResponseMonthlyAmount;
import pb.card.Card.CardResponseYearlyAmount;
import pb.card.Card.FindYearAmount;
import pb.card.Card.FindYearAmountCardNumber;

@GrpcService
@Singleton
public class CardStatsWithdrawGrpcHandler extends MutinyCardStatsWithdrawServiceGrpc.CardStatsWithdrawServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyWithdrawAmount(FindYearAmount request) {
        return repo.monthlyAmounts("withdraw_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly withdraw amounts");
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
    public Uni<ApiResponseYearlyAmount> findYearlyWithdrawAmount(FindYearAmount request) {
        return repo.yearlyAmounts("withdraw_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly withdraw amounts");
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
    public Uni<ApiResponseMonthlyAmount> findMonthlyWithdrawAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.monthlyAmounts("withdraw_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly withdraw amounts by card");
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
    public Uni<ApiResponseYearlyAmount> findYearlyWithdrawAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.yearlyAmounts("withdraw_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly withdraw amounts by card");
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
