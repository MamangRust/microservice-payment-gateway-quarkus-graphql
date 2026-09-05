package com.sanedge.card.service;

import java.util.List;

import com.sanedge.card.domain.requests.FindAllCards;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CardQueryService {
    Uni<ApiResponsePagination<List<CardResponse>>> findAll(FindAllCards req);
    Uni<ApiResponsePagination<List<CardResponseDeleteAt>>> findByActive(FindAllCards req);
    Uni<ApiResponsePagination<List<CardResponseDeleteAt>>> findByTrashed(FindAllCards req);
    Uni<ApiResponse<CardResponse>> findById(Long cardId);
    Uni<ApiResponse<CardResponse>> findByUserId(Long userId);
    Uni<ApiResponse<CardResponse>> findByCardNumber(String cardNumber);
}
