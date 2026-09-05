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

import pb.merchant.stats.MutinyMerchantStatsTotalAmountServiceGrpc;
import pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantMonthlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.ApiResponseMerchantYearlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseMonthlyTotalAmount;
import pb.merchant.stats.MerchantStatsTotalamount.MerchantResponseYearlyTotalAmount;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.Merchant.FindYearMerchantByApikey;

@GrpcService
@Singleton
public class MerchantStatsTotalAmountGrpcHandler extends MutinyMerchantStatsTotalAmountServiceGrpc.MerchantStatsTotalAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountMerchant(FindYearMerchant request) {
        return repo.monthlyAmounts("transaction_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyTotalAmount.Builder b = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant total amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyTotalAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setYear(strOf(r, "year"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountMerchant(FindYearMerchant request) {
        return repo.yearlyAmounts("transaction_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyTotalAmount.Builder b = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant total amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyTotalAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByMerchants(FindYearMerchantById request) {
        return repo.monthlyAmounts("transaction_events", "merchant_id", request.getMerchantId(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyTotalAmount.Builder b = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant total amounts by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyTotalAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setYear(strOf(r, "year"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByMerchants(FindYearMerchantById request) {
        return repo.yearlyAmounts("transaction_events", "merchant_id", request.getMerchantId(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyTotalAmount.Builder b = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant total amounts by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyTotalAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyTotalAmount> findMonthlyTotalAmountByApikey(FindYearMerchantByApikey request) {
        return repo.monthlyAmounts("transaction_events", "apikey", request.getApiKey(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyTotalAmount.Builder b = ApiResponseMerchantMonthlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant total amounts by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyTotalAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setYear(strOf(r, "year"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyTotalAmount> findYearlyTotalAmountByApikey(FindYearMerchantByApikey request) {
        return repo.yearlyAmounts("transaction_events", "apikey", request.getApiKey(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyTotalAmount.Builder b = ApiResponseMerchantYearlyTotalAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant total amounts by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyTotalAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
