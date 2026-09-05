package com.sanedge.card.service;

import com.sanedge.card.domain.requests.CreateCardRequest;
import com.sanedge.card.domain.requests.UpdateCardRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface CardCommandService {
    Uni<ApiResponse<CardResponse>> createCard(CreateCardRequest req);
    Uni<ApiResponse<CardResponse>> updateCard(UpdateCardRequest req);
    Uni<ApiResponse<CardResponseDeleteAt>> trashCard(Long id);
    Uni<ApiResponse<CardResponseDeleteAt>> restoreCard(Long id);
    Uni<ApiResponse<Boolean>> deleteCard(Long id);
    Uni<ApiResponse<Boolean>> restoreAll();
    Uni<ApiResponse<Boolean>> deleteAll();
}
