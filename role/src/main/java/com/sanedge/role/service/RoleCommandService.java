package com.sanedge.role.service;

import com.sanedge.role.domain.requests.CreateRoleRequest;
import com.sanedge.role.domain.requests.UpdateRoleRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;

import io.smallrye.mutiny.Uni;

public interface RoleCommandService {
    Uni<ApiResponse<RoleResponse>> create(CreateRoleRequest request);
    Uni<ApiResponse<RoleResponse>> update(UpdateRoleRequest request);
    Uni<ApiResponse<RoleResponseDeleteAt>> trash(Long id);
    Uni<ApiResponse<RoleResponseDeleteAt>> restore(Long id);
    Uni<ApiResponse<Void>> deletePermanent(Long id);
    Uni<ApiResponse<Void>> restoreAllTrashedRoles();
    Uni<ApiResponse<Void>> deleteAllTrashedRoles();
    Uni<ApiResponse<UserRoleResponse>> assignRoleToUser(Long userId, Long roleId);
    Uni<ApiResponse<Void>> removeRoleFromUser(Long userId, Long roleId);
}
