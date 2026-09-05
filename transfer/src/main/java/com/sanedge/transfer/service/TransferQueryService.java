package com.sanedge.transfer.service;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.transfer.domain.requests.FindAllTransfers;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface TransferQueryService {
    Uni<ApiResponsePagination<List<TransferResponse>>> findAll(FindAllTransfers req);

    Uni<ApiResponse<TransferResponse>> findById(Long transferId);

    Uni<ApiResponsePagination<List<TransferResponseDeleteAt>>> findByActive(FindAllTransfers req);

    Uni<ApiResponsePagination<List<TransferResponseDeleteAt>>> findByTrashed(FindAllTransfers req);

    Uni<ApiResponse<List<TransferResponse>>> findByTransferFrom(String transferFrom);

    Uni<ApiResponse<List<TransferResponse>>> findByTransferTo(String transferTo);
}
