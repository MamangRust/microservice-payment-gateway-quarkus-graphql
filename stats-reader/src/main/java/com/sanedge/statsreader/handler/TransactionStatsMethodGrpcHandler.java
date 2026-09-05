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

import pb.transaction.stats.MutinyTransactionStatsMethodServiceGrpc;
import pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionMonthMethod;
import pb.transaction.stats.TransactionStatsMethod.ApiResponseTransactionYearMethod;
import pb.transaction.stats.TransactionStatsMethod.TransactionMonthMethodResponse;
import pb.transaction.stats.TransactionStatsMethod.TransactionYearMethodResponse;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;

@GrpcService
@Singleton
public class TransactionStatsMethodGrpcHandler extends MutinyTransactionStatsMethodServiceGrpc.TransactionStatsMethodServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyPaymentMethods(FindYearTransactionStatus request) {
        return repo.monthlyMethodStats("transaction_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseTransactionMonthMethod.Builder b = ApiResponseTransactionMonthMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthMethodResponse.newBuilder()
                                .setMonth(strOf(r, "month"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearMethod> findYearlyPaymentMethods(FindYearTransactionStatus request) {
        return repo.yearlyMethodStats("transaction_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTransactionYearMethod.Builder b = ApiResponseTransactionYearMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly transaction methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearMethodResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionMonthMethod> findMonthlyPaymentMethodsByCardNumber(FindByYearCardNumberTransactionRequest request) {
        return repo.monthlyMethodStats("transaction_events", "card_number", request.getCardNumber(), request.getYear())
                .map(rows -> {
                    ApiResponseTransactionMonthMethod.Builder b = ApiResponseTransactionMonthMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly transaction methods by card number");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionMonthMethodResponse.newBuilder()
                                .setMonth(strOf(r, "month"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransactionYearMethod> findYearlyPaymentMethodsByCardNumber(FindByYearCardNumberTransactionRequest request) {
        return repo.yearlyMethodStats("transaction_events", "card_number", request.getCardNumber(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseTransactionYearMethod.Builder b = ApiResponseTransactionYearMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly transaction methods by card number");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(TransactionYearMethodResponse.newBuilder()
                                .setYear(strOf(r, "year"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setTotalTransactions(intOf(r, "total_transactions"))
                                .setTotalAmount(intOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
