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

import pb.card.stats.MutinyCardStatsTopupServiceGrpc;
import pb.card.Card.ApiResponseMonthlyAmount;
import pb.card.Card.ApiResponseYearlyAmount;
import pb.card.Card.CardResponseMonthlyAmount;
import pb.card.Card.CardResponseYearlyAmount;
import pb.card.Card.FindYearAmount;
import pb.card.Card.FindYearAmountCardNumber;

@GrpcService
@Singleton
public class CardStatsTopupGrpcHandler extends MutinyCardStatsTopupServiceGrpc.CardStatsTopupServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMonthlyAmount> findMonthlyTopupAmount(FindYearAmount request) {
        return repo.monthlyAmounts("topup_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly topup amounts");
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
    public Uni<ApiResponseYearlyAmount> findYearlyTopupAmount(FindYearAmount request) {
        return repo.yearlyAmounts("topup_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly topup amounts");
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
    public Uni<ApiResponseMonthlyAmount> findMonthlyTopupAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.monthlyAmounts("topup_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseMonthlyAmount.Builder b = ApiResponseMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly topup amounts by card");
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
    public Uni<ApiResponseYearlyAmount> findYearlyTopupAmountByCardNumber(FindYearAmountCardNumber request) {
        return repo.yearlyAmounts("topup_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseYearlyAmount.Builder b = ApiResponseYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly topup amounts by card");
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
