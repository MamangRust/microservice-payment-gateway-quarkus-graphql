package com.sanedge.merchant.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest;
import com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantDocumentCommandService {
    Uni<ApiResponse<MerchantDocumentResponse>> create(CreateMerchantDocumentRequest req);

    Uni<ApiResponse<MerchantDocumentResponse>> update(UpdateMerchantDocumentRequest req);

    Uni<ApiResponse<MerchantDocumentResponse>> updateStatus(UpdateMerchantDocumentStatus req);

    Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> trash(Long id);

    Uni<ApiResponse<MerchantDocumentResponseDeleteAt>> restore(Long id);

    Uni<ApiResponse<Boolean>> deletePermanent(Long id);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAllPermanent();
}
