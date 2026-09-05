package com.sanedge.user.service;

import java.util.List;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface UserQueryService {
    Uni<ApiResponsePagination<List<UserResponse>>> findAllPaginated(FindAllUsers request);
    Uni<ApiResponsePagination<List<UserResponseDeleteAt>>> findActivePaginated(FindAllUsers request);
    Uni<ApiResponsePagination<List<UserResponseDeleteAt>>> findTrashedPaginated(FindAllUsers request);
    Uni<ApiResponse<UserResponse>> findById(Long id);
}
