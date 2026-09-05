package com.sanedge.role.service;

import java.util.List;

import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;

import io.smallrye.mutiny.Uni;

public interface RoleQueryService {
    Uni<ApiResponsePagination<List<RoleResponse>>> findAllPaginated(FindAllRoles request);
    Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findActivePaginated(FindAllRoles request);
    Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findTrashedPaginated(FindAllRoles request);
    Uni<ApiResponse<RoleResponse>> findById(Long id);
    Uni<ApiResponse<RoleResponse>> findByName(String name);
    Uni<ApiResponse<List<RoleResponse>>> findByUserId(Long userId);
}

