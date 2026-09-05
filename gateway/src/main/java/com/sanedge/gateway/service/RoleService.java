package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.RoleDto.*;
import io.smallrye.mutiny.Uni;

public interface RoleService {
    Uni<FindAllRoleResponse> listRoles(int page, int size, String search);
    Uni<FindByIdRoleResponse> getRole(int id);
    Uni<FindByIdRoleResponse> getRoleByName(String name);
    Uni<FindAllRoleResponse> findActiveRoles(int page, int size, String search);
    Uni<FindAllRoleResponse> findTrashedRoles(int page, int size, String search);
    Uni<FindAllRoleResponse> findRolesByUserId(int userId);
    Uni<CreateRoleResponse> createRole(CreateRoleRequest body);
    Uni<UpdateRoleResponse> updateRole(int id, UpdateRoleRequest body);
    Uni<SimpleStatusMessageResponse> deleteRolePermanent(int id);
    Uni<TrashedRoleResponse> deleteRole(int id);
    Uni<TrashedRoleResponse> trashRole(int id);
    Uni<TrashedRoleResponse> restoreRole(int id);
    Uni<SimpleStatusMessageResponse> restoreAllRoles();
    Uni<SimpleStatusMessageResponse> deleteAllRoles();
    Uni<AssignRoleToUserResponse> assignRoleToUser(int userId, int roleId);
    Uni<SimpleStatusMessageResponse> removeRoleFromUser(int userId, int roleId);
}
