package com.sanedge.card.service;

import java.time.LocalDate;
import java.util.List;

import com.sanedge.card.domain.response.BillingStatementResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.smallrye.mutiny.Uni;

public interface BillingEngineService {
    Uni<ApiResponse<Integer>> triggerBillingCycle(int billingCycleDay);
    Uni<ApiResponse<BillingStatementResponse>> getStatement(String cardNumber, LocalDate statementDate);
    Uni<ApiResponsePagination<List<BillingStatementResponse>>> getStatementsByCard(String cardNumber, int page, int size);
}
