package com.sanedge.common.adapter.transaction;

import java.util.List;

import com.sanedge.common.adapter.model.Transaction;
import com.sanedge.common.adapter.support.Paged;

import io.smallrye.mutiny.Uni;

/**
 * Port for reading transactions owned by the transaction service, replacing
 * direct {@code @GrpcClient("transaction")} usage in consumer services.
 */
public interface TransactionPort {

    Uni<Paged<Transaction>> findAll(int page, int pageSize, String search);

    Uni<List<Transaction>> findByMerchantId(int merchantId);
}
