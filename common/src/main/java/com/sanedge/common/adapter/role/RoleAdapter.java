package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.RoleResponse;
import pb.role.RoleQuery.FindByNameRoleRequest;
import pb.role.RoleService;

@ApplicationScoped
public class RoleAdapter implements RolePort {

    @GrpcClient("role")
    RoleService query;

    @Override
    public Uni<Role> findById(int roleId) {
        return query.findByIdRole(FindByIdRoleRequest.newBuilder().setRoleId(roleId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Role not found: " + roleId);
                    }
                    return toRole(resp.getData());
                });
    }

    @Override
    public Uni<Role> findByName(String name) {
        return query.findByNameRole(FindByNameRoleRequest.newBuilder().setName(name).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Role not found: " + name);
                    }
                    return toRole(resp.getData());
                });
    }

    static Role toRole(RoleResponse r) {
        if (r == null) {
            return null;
        }
        return new Role(r.getId(), r.getName(), ProtoTime.parse(r.getCreatedAt()),
                ProtoTime.parse(r.getUpdatedAt()));
    }
}
