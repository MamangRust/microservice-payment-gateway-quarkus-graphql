package com.sanedge.gateway.resource;

import com.sanedge.gateway.dto.RoleDto.*;
import com.sanedge.gateway.service.RoleService;
import io.smallrye.mutiny.Uni;
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import org.eclipse.microprofile.graphql.*;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class RoleResource {

    @Inject
    RoleService roleService;

    @Query("listRoles")
    @Description("List all roles")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllRoleResponse> listRoles(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return roleService.listRoles(page, size, search);
    }

    @Query("getRole")
    @Description("Get role by ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindByIdRoleResponse> getRole(@Name("id") int id) {
        return roleService.getRole(id);
    }

    @Query("getRoleByName")
    @Description("Get role by name")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindByIdRoleResponse> getRoleByName(@Name("name") String name) {
        return roleService.getRoleByName(name);
    }

    @Query("findActiveRoles")
    @Description("List active roles")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllRoleResponse> findActiveRoles(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return roleService.findActiveRoles(page, size, search);
    }

    @Query("findTrashedRoles")
    @Description("List trashed roles")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllRoleResponse> findTrashedRoles(
            @Name("page") @DefaultValue("1") int page,
            @Name("size") @DefaultValue("20") int size,
            @Name("search") String search) {
        return roleService.findTrashedRoles(page, size, search);
    }

    @Query("findRolesByUserId")
    @Description("Find roles associated with a User ID")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF"})
    public Uni<FindAllRoleResponse> findRolesByUserId(@Name("userId") int userId) {
        return roleService.findRolesByUserId(userId);
    }

    @Mutation("createRole")
    @Description("Create a new role")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<CreateRoleResponse> createRole(@Name("body") CreateRoleRequest body) {
        return roleService.createRole(body);
    }

    @Mutation("updateRole")
    @Description("Update role")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<UpdateRoleResponse> updateRole(@Name("id") int id, @Name("body") UpdateRoleRequest body) {
        return roleService.updateRole(id, body);
    }

    @Mutation("deleteRolePermanent")
    @Description("Permanently delete role by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteRolePermanent(@Name("id") int id) {
        return roleService.deleteRolePermanent(id);
    }

    @Mutation("deleteRole")
    @Description("Soft-delete a role")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedRoleResponse> deleteRole(@Name("id") int id) {
        return roleService.deleteRole(id);
    }

    @Mutation("trashRole")
    @Description("Soft-delete role by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedRoleResponse> trashRole(@Name("id") int id) {
        return roleService.trashRole(id);
    }

    @Mutation("restoreRole")
    @Description("Restore role by ID")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<TrashedRoleResponse> restoreRole(@Name("id") int id) {
        return roleService.restoreRole(id);
    }

    @Mutation("restoreAllRoles")
    @Description("Restore all roles")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> restoreAllRoles() {
        return roleService.restoreAllRoles();
    }

    @Mutation("deleteAllRoles")
    @Description("Delete all roles permanently")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> deleteAllRoles() {
        return roleService.deleteAllRoles();
    }

    @Mutation("assignRoleToUser")
    @Description("Assign a role to a user")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<AssignRoleToUserResponse> assignRoleToUser(
            @Name("userId") int userId,
            @Name("roleId") int roleId) {
        return roleService.assignRoleToUser(userId, roleId);
    }

    @Mutation("removeRoleFromUser")
    @Description("Remove a role from a user")
    @RolesAllowed({"ROLE_ADMIN"})
    public Uni<SimpleStatusMessageResponse> removeRoleFromUser(
            @Name("userId") int userId,
            @Name("roleId") int roleId) {
        return roleService.removeRoleFromUser(userId, roleId);
    }
}
