package com.sanedge.card.service;

import java.math.BigDecimal;
import java.util.List;

import com.sanedge.card.domain.response.CardRewardResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;

import io.smallrye.mutiny.Uni;

public interface CardRewardService {
    Uni<ApiResponse<CardRewardResponse>> earnRewards(String cardNumber, Long authTxnId, BigDecimal amount, String mcc);
    Uni<ApiResponse<BigDecimal>> getBalance(String cardNumber);
    Uni<ApiResponsePagination<List<CardRewardResponse>>> getHistory(String cardNumber, int page, int size);
    Uni<ApiResponse<CardRewardResponse>> redeemRewards(String cardNumber, BigDecimal points);
}
