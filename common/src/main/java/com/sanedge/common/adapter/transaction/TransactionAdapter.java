package com.sanedge.common.adapter.transaction;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.Transaction;
import com.sanedge.common.adapter.support.Paged;
import com.sanedge.common.adapter.support.ProtoTime;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.transaction.Transaction.TransactionResponse;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;
import pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest;
import pb.transaction.TransactionQueryService;

@ApplicationScoped
public class TransactionAdapter implements TransactionPort {

    @GrpcClient("transaction")
    TransactionQueryService query;

    @Override
    public Uni<Paged<Transaction>> findAll(int page, int pageSize, String search) {
        return query.findAllTransaction(FindAllTransactionRequest.newBuilder()
                .setPage(page)
                .setPageSize(pageSize)
                .setSearch(nullSafe(search))
                .build())
                .map(resp -> {
                    if (resp == null) {
                        return Paged.empty();
                    }
                    int total = resp.hasPaginationMeta() ? resp.getPaginationMeta().getTotalRecords() : resp.getDataCount();
                    return Paged.of(toList(resp.getDataList()), total);
                });
    }

    @Override
    public Uni<List<Transaction>> findByMerchantId(int merchantId) {
        return query.findTransactionByMerchantId(
                FindTransactionByMerchantIdRequest.newBuilder().setMerchantId(merchantId).build())
                .map(resp -> resp == null ? List.of() : toList(resp.getDataList()));
    }

    private static List<Transaction> toList(List<TransactionResponse> rows) {
        List<Transaction> out = new ArrayList<>(rows.size());
        for (TransactionResponse r : rows) {
            if (r == null) {
                continue;
            }
            out.add(new Transaction(r.getId(), r.getCardNumber(), r.getTransactionNo(), r.getAmount(),
                    r.getPaymentMethod(), r.getMerchantId(), ProtoTime.parse(r.getTransactionTime()),
                    ProtoTime.parse(r.getCreatedAt()), ProtoTime.parse(r.getUpdatedAt())));
        }
        return out;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
