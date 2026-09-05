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

import pb.merchant.stats.MutinyMerchantStatsAmountServiceGrpc;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.ApiResponseMerchantYearlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseMonthlyAmount;
import pb.merchant.stats.MerchantStatsAmount.MerchantResponseYearlyAmount;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.Merchant.FindYearMerchantByApikey;

@GrpcService
@Singleton
public class MerchantStatsAmountGrpcHandler extends MutinyMerchantStatsAmountServiceGrpc.MerchantStatsAmountServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> findMonthlyAmountMerchant(FindYearMerchant request) {
        return repo.monthlyAmounts("transaction_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyAmount.Builder b = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> findYearlyAmountMerchant(FindYearMerchant request) {
        return repo.yearlyAmounts("transaction_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyAmount.Builder b = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant amounts");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByMerchants(FindYearMerchantById request) {
        return repo.monthlyAmounts("transaction_events", "merchant_id", request.getMerchantId(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyAmount.Builder b = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant amounts by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> findYearlyAmountByMerchants(FindYearMerchantById request) {
        return repo.yearlyAmounts("transaction_events", "merchant_id", request.getMerchantId(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyAmount.Builder b = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant amounts by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyAmount> findMonthlyAmountByApikey(FindYearMerchantByApikey request) {
        return repo.monthlyAmounts("transaction_events", "apikey", request.getApiKey(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyAmount.Builder b = ApiResponseMerchantMonthlyAmount.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant amounts by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyAmount.newBuilder()
                                .setMonth(strOf(r, "month")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyAmount> findYearlyAmountByApikey(FindYearMerchantByApikey request) {
        return repo.yearlyAmounts("transaction_events", "apikey", request.getApiKey(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyAmount.Builder b = ApiResponseMerchantYearlyAmount.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant amounts by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyAmount.newBuilder()
                                .setYear(strOf(r, "year")).setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
