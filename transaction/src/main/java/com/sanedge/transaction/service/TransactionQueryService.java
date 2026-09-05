package com.sanedge.transaction.service;

import java.util.List;

import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface TransactionQueryService {
    Uni<ApiResponsePagination<List<TransactionResponse>>> findAll(FindAllTransactions req);

    Uni<ApiResponsePagination<List<TransactionResponse>>> findAllByCardNumber(FindAllTransactionCardNumber req);

    Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActive(FindAllTransactions req);

    Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashed(FindAllTransactions req);

    Uni<ApiResponse<TransactionResponse>> findById(Long transactionId);

    Uni<ApiResponse<List<TransactionResponse>>> findByMerchantId(Long merchantId);
}
