package com.sanedge.common.adapter.role;

import java.util.ArrayList;
import java.util.List;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.UserRole;
import com.sanedge.common.adapter.support.AdapterException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.Role.RoleResponse;
import pb.role.RoleCommand.AssignRoleToUserRequest;
import pb.role.RoleCommand.RemoveRoleFromUserRequest;
import pb.role.RoleCommandService;
import pb.role.RoleService;

@ApplicationScoped
public class UserRoleAdapter implements UserRolePort {

    @GrpcClient("role")
    RoleService query;

    @GrpcClient("role")
    RoleCommandService command;

    @Override
    public Uni<List<Role>> findByUserId(int userId) {
        return query.findByUserId(FindByIdUserRoleRequest.newBuilder().setUserId(userId).build())
                .map(resp -> {
                    if (resp == null) {
                        return List.of();
                    }
                    List<Role> roles = new ArrayList<>(resp.getDataCount());
                    for (RoleResponse r : resp.getDataList()) {
                        if (r != null) {
                            roles.add(RoleAdapter.toRole(r));
                        }
                    }
                    return roles;
                });
    }

    @Override
    public Uni<UserRole> assignRoleToUser(int userId, int roleId) {
        return command.assignRoleToUser(AssignRoleToUserRequest.newBuilder()
                .setUserId(userId)
                .setRoleId(roleId)
                .build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new AdapterException("assign role to user failed");
                    }
                    return new UserRole(resp.getData().getUserRoleId(), resp.getData().getUserId(),
                            resp.getData().getRoleId());
                });
    }

    @Override
    public Uni<Void> removeRoleFromUser(int userId, int roleId) {
        return command.removeRoleFromUser(RemoveRoleFromUserRequest.newBuilder()
                .setUserId(userId)
                .setRoleId(roleId)
                .build())
                .replaceWithVoid();
    }
}
