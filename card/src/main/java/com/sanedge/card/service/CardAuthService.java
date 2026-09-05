package com.sanedge.card.service;

import com.sanedge.card.domain.requests.AuthorizeCardRequest;
import com.sanedge.card.domain.requests.ReverseTransactionRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.card.domain.response.CardAuthTransactionResponse;

import io.smallrye.mutiny.Uni;

public interface CardAuthService {
    Uni<ApiResponse<CardAuthTransactionResponse>> authorize(AuthorizeCardRequest req);
    Uni<ApiResponse<CardAuthTransactionResponse>> reverse(ReverseTransactionRequest req);
}
