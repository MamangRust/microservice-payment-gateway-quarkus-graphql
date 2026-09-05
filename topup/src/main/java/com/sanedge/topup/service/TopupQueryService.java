package com.sanedge.topup.service;

import java.util.List;

import com.sanedge.topup.domain.requests.FindAllTopups;
import com.sanedge.topup.domain.requests.FindAllTopupsByCardNumber;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface TopupQueryService {
    Uni<ApiResponsePagination<List<TopupResponse>>> findAll(FindAllTopups req);

    Uni<ApiResponsePagination<List<TopupResponse>>> findAllByCardNumber(FindAllTopupsByCardNumber req);

    Uni<ApiResponsePagination<List<TopupResponseDeleteAt>>> findActive(FindAllTopups req);

    Uni<ApiResponsePagination<List<TopupResponseDeleteAt>>> findTrashed(FindAllTopups req);

    Uni<ApiResponse<List<TopupResponse>>> findByCard(String cardNumber);

    Uni<ApiResponse<TopupResponse>> findById(Long topupId);
}
