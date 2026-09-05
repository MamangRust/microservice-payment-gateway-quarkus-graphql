package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.RoleDto.*;
import com.sanedge.gateway.service.RoleService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RoleServiceImpl implements RoleService {

    private static final Logger LOG = Logger.getLogger(RoleServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("role")
    pb.role.MutinyRoleServiceGrpc.MutinyRoleServiceStub roleQueryService;

    @GrpcClient("role")
    pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    @Override
    public Uni<FindAllRoleResponse> listRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.listRoles", () -> roleQueryService.findAllRole(
                pb.role.Role.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdRoleResponse> getRole(int id) {
        return telemetryHelper.traceAndMetric("role.getRole", () -> roleQueryService.findByIdRole(
                pb.role.Role.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(FindByIdRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdRoleResponse> getRoleByName(String name) {
        return telemetryHelper.traceAndMetric("role.getRoleByName", () -> roleQueryService.findByNameRole(
                pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                        .setName(name == null ? "" : name)
                        .build())
                .map(FindByIdRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get role by name: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllRoleResponse> findActiveRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.findActiveRoles", () -> roleQueryService.findByActive(
                pb.role.Role.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find active roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllRoleResponse> findTrashedRoles(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("role.findTrashedRoles", () -> roleQueryService.findByTrashed(
                pb.role.Role.FindAllRoleRequest.newBuilder()
                        .setPage(page)
                        .setPageSize(size)
                        .setSearch(search == null ? "" : search)
                        .build())
                .map(FindAllRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find trashed roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindAllRoleResponse> findRolesByUserId(int userId) {
        return telemetryHelper.traceAndMetric("role.findRolesByUserId", () -> roleQueryService.findByUserId(
                pb.role.Role.FindByIdUserRoleRequest.newBuilder()
                        .setUserId(userId)
                        .build())
                .map(FindAllRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to find roles by user id: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateRoleResponse> createRole(CreateRoleRequest body) {
        return telemetryHelper.traceAndMetric("role.createRole", () -> roleCommandService.createRole(
                pb.role.RoleCommand.CreateRoleRequest.newBuilder()
                        .setName(body.name() == null ? "" : body.name())
                        .build())
                .map(CreateRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateRoleResponse> updateRole(int id, UpdateRoleRequest body) {
        return telemetryHelper.traceAndMetric("role.updateRole", () -> roleCommandService.updateRole(
                pb.role.RoleCommand.UpdateRoleRequest.newBuilder()
                        .setId(id)
                        .setName(body.name() == null ? "" : body.name())
                        .build())
                .map(UpdateRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteRolePermanent(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRolePermanent", () -> roleCommandService.deleteRolePermanent(
                pb.role.Role.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedRoleResponse> deleteRole(int id) {
        return telemetryHelper.traceAndMetric("role.deleteRole", () -> roleCommandService.trashedRole(
                pb.role.Role.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(TrashedRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedRoleResponse> trashRole(int id) {
        return telemetryHelper.traceAndMetric("role.trashRole", () -> roleCommandService.trashedRole(
                pb.role.Role.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(TrashedRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to trash role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedRoleResponse> restoreRole(int id) {
        return telemetryHelper.traceAndMetric("role.restoreRole", () -> roleCommandService.restoreRole(
                pb.role.Role.FindByIdRoleRequest.newBuilder()
                        .setRoleId(id)
                        .build())
                .map(TrashedRoleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> restoreAllRoles() {
        return telemetryHelper.traceAndMetric("role.restoreAllRoles", () -> roleCommandService.restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to restore all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> deleteAllRoles() {
        return telemetryHelper.traceAndMetric("role.deleteAllRoles", () -> roleCommandService.deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance())
                .map(SimpleStatusMessageResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to permanently delete all roles: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<AssignRoleToUserResponse> assignRoleToUser(int userId, int roleId) {
        return telemetryHelper.traceAndMetric("role.assignRoleToUser", () -> roleCommandService.assignRoleToUser(
                pb.role.RoleCommand.AssignRoleToUserRequest.newBuilder()
                        .setUserId(userId)
                        .setRoleId(roleId)
                        .build())
                .map(AssignRoleToUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to assign role: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleStatusMessageResponse> removeRoleFromUser(int userId, int roleId) {
        return telemetryHelper.traceAndMetric("role.removeRoleFromUser", () -> roleCommandService.removeRoleFromUser(
                pb.role.RoleCommand.RemoveRoleFromUserRequest.newBuilder()
                        .setUserId(userId)
                        .setRoleId(roleId)
                        .build())
                .map(empty -> SimpleStatusMessageResponse.success("Role removed successfully"))
                .onFailure().invoke(throwable -> LOG.error("Failed to remove role: " + throwable.getMessage(), throwable)));
    }
}
