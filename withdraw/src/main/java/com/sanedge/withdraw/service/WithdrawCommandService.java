package com.sanedge.withdraw.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.withdraw.domain.requests.CreateWithdrawRequest;
import com.sanedge.withdraw.domain.requests.UpdateWithdrawRequest;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface WithdrawCommandService {
    Uni<ApiResponse<WithdrawResponse>> create(CreateWithdrawRequest req);

    Uni<ApiResponse<WithdrawResponse>> update(UpdateWithdrawRequest req);

    Uni<ApiResponse<WithdrawResponseDeleteAt>> trashed(Long withdrawId);

    Uni<ApiResponse<WithdrawResponseDeleteAt>> restore(Long withdrawId);

    Uni<ApiResponse<Boolean>> deletePermanent(Long withdrawId);

    Uni<ApiResponse<Boolean>> restoreAll();

    Uni<ApiResponse<Boolean>> deleteAll();
}
