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

import pb.common.PaginationMeta;
import pb.merchant.MutinyMerchantTransactionServiceGrpc;
import pb.merchant.MerchantTransaction.ApiResponsePaginationMerchantTransaction;
import pb.merchant.MerchantTransaction.MerchantTransactionResponse;
import pb.merchant.Merchant.FindAllMerchantTransaction;
import pb.merchant.Merchant.FindAllMerchantTransactionApikey;
import pb.merchant.Merchant.FindAllMerchantTransactionId;

@GrpcService
@Singleton
public class MerchantTransactionGrpcHandler extends MutinyMerchantTransactionServiceGrpc.MerchantTransactionServiceImplBase {

    @Inject
    ClickHouseStatsRepository repo;

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findAllTransactionMerchant(FindAllMerchantTransaction request) {
        return repo.merchantTransactions(request.getMerchantId() != 0 ? request.getMerchantId() : null, null)
                .map(rows -> {
                    ApiResponsePaginationMerchantTransaction.Builder b = ApiResponsePaginationMerchantTransaction.newBuilder().setStatus("success").setMessage("Retrieved merchant transactions");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantTransactionResponse.newBuilder()
                                .setId(intOf(r, "transaction_id"))
                                .setAmount(intOf(r, "amount"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setCreatedAt(strOf(r, "created_at")));
                    }
                    b.setPaginationMeta(PaginationMeta.newBuilder()
                            .setCurrentPage(request.getPage()).setPageSize(request.getPageSize())
                            .setTotalRecords(rows.size()).setTotalPages(rows.isEmpty() ? 0 : 1));
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findAllTransactionByApikey(FindAllMerchantTransactionApikey request) {
        return repo.merchantTransactions(null, request.getApiKey())
                .map(rows -> {
                    ApiResponsePaginationMerchantTransaction.Builder b = ApiResponsePaginationMerchantTransaction.newBuilder().setStatus("success").setMessage("Retrieved merchant transactions by apikey");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantTransactionResponse.newBuilder()
                                .setId(intOf(r, "transaction_id"))
                                .setAmount(intOf(r, "amount"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setCreatedAt(strOf(r, "created_at")));
                    }
                    b.setPaginationMeta(PaginationMeta.newBuilder()
                            .setCurrentPage(request.getPage()).setPageSize(request.getPageSize())
                            .setTotalRecords(rows.size()).setTotalPages(rows.isEmpty() ? 0 : 1));
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationMerchantTransaction> findAllTransactionByMerchant(FindAllMerchantTransactionId request) {
        int merchantId = 0;
        try {
            merchantId = Integer.parseInt(request.getId());
        } catch (NumberFormatException ignored) { }
        final int mid = merchantId;
        return repo.merchantTransactions(mid != 0 ? mid : null, null)
                .map(rows -> {
                    ApiResponsePaginationMerchantTransaction.Builder b = ApiResponsePaginationMerchantTransaction.newBuilder().setStatus("success").setMessage("Retrieved merchant transactions by merchant");
                    for (Object o : rows) {
                        JsonObject r = (JsonObject) o;
                        b.addData(MerchantTransactionResponse.newBuilder()
                                .setId(intOf(r, "transaction_id"))
                                .setAmount(intOf(r, "amount"))
                                .setPaymentMethod(strOf(r, "method"))
                                .setCreatedAt(strOf(r, "created_at")));
                    }
                    b.setPaginationMeta(PaginationMeta.newBuilder()
                            .setCurrentPage(request.getPage()).setPageSize(request.getPageSize())
                            .setTotalRecords(rows.size()).setTotalPages(rows.isEmpty() ? 0 : 1));
                    return b.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }
}
