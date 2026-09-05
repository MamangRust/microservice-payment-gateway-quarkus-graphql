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

import pb.transaction.stats.MutinyTransactionStatsAmountServiceGrpc;


@GrpcService
@Singleton
public class TransactionStatsAmountGrpcHandler extends MutinyTransactionStatsAmountServiceGrpc.TransactionStatsAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmounts(pb.transaction.Transaction.FindYearTransactionStatus request) {
        return repo.monthlyAmounts("transaction_events", null, null, request.getYear())
                .map(rows -> {
                    pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.Builder b = pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transaction amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month"))
                                
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmounts(pb.transaction.Transaction.FindYearTransactionStatus request) {
        return repo.yearlyAmounts("transaction_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.Builder b = pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transaction amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount> findMonthlyAmountsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return repo.monthlyAmounts("transaction_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.Builder b = pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionMonthAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly transaction amounts by card number");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(pb.transaction.stats.TransactionStatsAmount.TransactionMonthAmountResponse.newBuilder()
                                .setMonth(strOf(r, "month"))
                                
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount> findYearlyAmountsByCardNumber(pb.transaction.Transaction.FindByYearCardNumberTransactionRequest request) {
        return repo.yearlyAmounts("transaction_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.Builder b = pb.transaction.stats.TransactionStatsAmount.ApiResponseTransactionYearAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly transaction amounts by card number");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(pb.transaction.stats.TransactionStatsAmount.TransactionYearlyAmountResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
