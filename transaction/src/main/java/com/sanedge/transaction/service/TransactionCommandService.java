package com.sanedge.transaction.service;

import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface TransactionCommandService {
    Uni<ApiResponse<TransactionResponse>> create(String apiKey, CreateTransactionRequest req);

    Uni<ApiResponse<TransactionResponse>> update(String apiKey, UpdateTransactionRequest req);

    Uni<ApiResponse<TransactionResponseDeleteAt>> trashed(Long transactionId);

    Uni<ApiResponse<TransactionResponseDeleteAt>> restore(Long transactionId);

    Uni<ApiResponse<Boolean>> deletePermanent(Long transactionId);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAll();
}
