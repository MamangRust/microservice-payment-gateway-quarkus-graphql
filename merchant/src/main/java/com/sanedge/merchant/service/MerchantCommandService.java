package com.sanedge.merchant.service;

import com.sanedge.merchant.domain.requests.CreateMerchantRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantCommandService {
    Uni<ApiResponse<MerchantResponse>> createMerchant(CreateMerchantRequest req);

    Uni<ApiResponse<MerchantResponse>> updateMerchant(UpdateMerchantRequest req);

    Uni<ApiResponse<MerchantResponseDeleteAt>> trashMerchant(Long id);

    Uni<ApiResponse<MerchantResponseDeleteAt>> restoreMerchant(Long id);

    Uni<ApiResponse<Boolean>> deleteMerchant(Long id);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAll();
}
