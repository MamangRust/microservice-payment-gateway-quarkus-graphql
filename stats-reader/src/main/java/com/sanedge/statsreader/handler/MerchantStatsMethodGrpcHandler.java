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

import pb.merchant.stats.MutinyMerchantStatsMethodServiceGrpc;
import pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantMonthlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.ApiResponseMerchantYearlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.MerchantResponseMonthlyPaymentMethod;
import pb.merchant.stats.MerchantStatsMethod.MerchantResponseYearlyPaymentMethod;
import pb.merchant.Merchant.FindYearMerchant;
import pb.merchant.Merchant.FindYearMerchantById;
import pb.merchant.Merchant.FindYearMerchantByApikey;

@GrpcService
@Singleton
public class MerchantStatsMethodGrpcHandler extends MutinyMerchantStatsMethodServiceGrpc.MerchantStatsMethodServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodsMerchant(FindYearMerchant request) {
        return repo.monthlyMethodStats("transaction_events", null, null, request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyPaymentMethod.Builder b = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant payment methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyPaymentMethod.newBuilder()
                                .setMonth(strOf(r, "month")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodMerchant(FindYearMerchant request) {
        return repo.yearlyMethodStats("transaction_events", null, null, request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyPaymentMethod.Builder b = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant payment methods");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyPaymentMethod.newBuilder()
                                .setYear(strOf(r, "year")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByMerchants(FindYearMerchantById request) {
        return repo.monthlyMethodStats("transaction_events", "merchant_id", request.getMerchantId(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyPaymentMethod.Builder b = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant payment methods by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyPaymentMethod.newBuilder()
                                .setMonth(strOf(r, "month")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByMerchants(FindYearMerchantById request) {
        return repo.yearlyMethodStats("transaction_events", "merchant_id", request.getMerchantId(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyPaymentMethod.Builder b = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant payment methods by id");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyPaymentMethod.newBuilder()
                                .setYear(strOf(r, "year")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantMonthlyPaymentMethod> findMonthlyPaymentMethodByApikey(FindYearMerchantByApikey request) {
        return repo.monthlyMethodStats("transaction_events", "apikey", request.getApiKey(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantMonthlyPaymentMethod.Builder b = ApiResponseMerchantMonthlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved monthly merchant payment methods by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseMonthlyPaymentMethod.newBuilder()
                                .setMonth(strOf(r, "month")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantYearlyPaymentMethod> findYearlyPaymentMethodByApikey(FindYearMerchantByApikey request) {
        return repo.yearlyMethodStats("transaction_events", "apikey", request.getApiKey(), request.getYear(), request.getYear())
                .map(rows -> {
                    ApiResponseMerchantYearlyPaymentMethod.Builder b = ApiResponseMerchantYearlyPaymentMethod.newBuilder().setStatus("success").setMessage("Retrieved yearly merchant payment methods by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantResponseYearlyPaymentMethod.newBuilder()
                                .setYear(strOf(r, "year")).setPaymentMethod(strOf(r, "method"))
                                .setTotalAmount(longOf(r, "total_amount")));
                    }
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
