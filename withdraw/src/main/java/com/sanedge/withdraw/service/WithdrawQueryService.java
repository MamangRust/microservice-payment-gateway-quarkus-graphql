package com.sanedge.withdraw.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface WithdrawQueryService {
    Uni<ApiResponsePagination<List<WithdrawResponse>>> findAll(FindAllWithdraws req);

    Uni<ApiResponsePagination<List<WithdrawResponse>>> findAllByCardNumber(FindAllWithdrawCardNumber req);

    Uni<ApiResponse<WithdrawResponse>> findById(Long withdrawId);

    Uni<ApiResponse<List<WithdrawResponse>>> findByCard(String cardNumber);

    Uni<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> findByActive(FindAllWithdraws req);

    Uni<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> findByTrashed(FindAllWithdraws req);
}
