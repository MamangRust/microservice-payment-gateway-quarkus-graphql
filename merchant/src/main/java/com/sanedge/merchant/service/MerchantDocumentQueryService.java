package com.sanedge.merchant.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface MerchantDocumentQueryService {
    Uni<ApiResponsePagination<List<MerchantDocumentResponse>>> findAll(FindAllMerchantDocuments req);

    Uni<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findAllActive(FindAllMerchantDocuments req);

    Uni<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findAllTrashed(FindAllMerchantDocuments req);

    Uni<ApiResponse<MerchantDocumentResponse>> findById(Long id);
}
