package com.sanedge.merchant.service;

import java.util.List;

import com.sanedge.merchant.domain.requests.FindAllMerchants;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantQueryService {
    Uni<ApiResponsePagination<List<MerchantResponse>>> findAll(FindAllMerchants req);

    Uni<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByActive(FindAllMerchants req);

    Uni<ApiResponsePagination<List<MerchantResponseDeleteAt>>> findByTrashed(FindAllMerchants req);

    Uni<ApiResponse<MerchantResponse>> findById(Long merchantId);

    Uni<ApiResponse<MerchantResponse>> findByApiKey(String apiKey);

    Uni<ApiResponse<List<MerchantResponse>>> findByUserId(Long userId);
}
