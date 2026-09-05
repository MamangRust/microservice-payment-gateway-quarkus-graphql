package com.sanedge.role.handler;

import com.google.protobuf.Empty;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;
import com.sanedge.role.service.RoleCommandService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.role.MutinyRoleCommandServiceGrpc;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponseRoleDeleteAt;
import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

@GrpcService
@Singleton
public class RoleCommandGrpcHandler extends MutinyRoleCommandServiceGrpc.RoleCommandServiceImplBase {

    @Inject
    RoleCommandService roleCommandService;

    @Override
    public Uni<ApiResponseRole> createRole(CreateRoleRequest request) {
        com.sanedge.role.domain.requests.CreateRoleRequest domainReq = new com.sanedge.role.domain.requests.CreateRoleRequest();
        domainReq.setName(request.getName());

        return roleCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseRole.Builder builder = ApiResponseRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRole> updateRole(UpdateRoleRequest request) {
        com.sanedge.role.domain.requests.UpdateRoleRequest domainReq = new com.sanedge.role.domain.requests.UpdateRoleRequest();
        domainReq.setRoleId(request.getId());
        domainReq.setName(request.getName());

        return roleCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseRole.Builder builder = ApiResponseRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRoleDeleteAt> trashedRole(FindByIdRoleRequest request) {
        return roleCommandService.trash((long) request.getRoleId())
                .map(apiResp -> {
                    ApiResponseRoleDeleteAt.Builder builder = ApiResponseRoleDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRoleDeleteAt> restoreRole(FindByIdRoleRequest request) {
        return roleCommandService.restore((long) request.getRoleId())
                .map(apiResp -> {
                    ApiResponseRoleDeleteAt.Builder builder = ApiResponseRoleDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRoleDelete> deleteRolePermanent(FindByIdRoleRequest request) {
        return roleCommandService.deletePermanent((long) request.getRoleId())
                .map(apiResp -> ApiResponseRoleDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRoleAll> restoreAllRole(Empty request) {
        return roleCommandService.restoreAllTrashedRoles()
                .map(apiResp -> ApiResponseRoleAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseRoleAll> deleteAllRolePermanent(Empty request) {
        return roleCommandService.deleteAllTrashedRoles()
                .map(apiResp -> ApiResponseRoleAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private pb.role.Role.RoleResponse toProto(RoleResponse r) {
        if (r == null) {
            return pb.role.Role.RoleResponse.getDefaultInstance();
        }
        pb.role.Role.RoleResponse.Builder builder = pb.role.Role.RoleResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.role.Role.RoleResponseDeleteAt toProto(RoleResponseDeleteAt r) {
        if (r == null) {
            return pb.role.Role.RoleResponseDeleteAt.getDefaultInstance();
        }
        pb.role.Role.RoleResponseDeleteAt.Builder builder = pb.role.Role.RoleResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt()));
        }
        return builder.build();
    }

    @Override
    public Uni<pb.role.RoleCommand.ApiResponseUserRole> assignRoleToUser(
            pb.role.RoleCommand.AssignRoleToUserRequest request) {
        return roleCommandService.assignRoleToUser((long) request.getUserId(), (long) request.getRoleId())
                .map(apiResp -> {
                    pb.role.RoleCommand.ApiResponseUserRole.Builder builder = pb.role.RoleCommand.ApiResponseUserRole
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<Empty> removeRoleFromUser(pb.role.RoleCommand.RemoveRoleFromUserRequest request) {
        return roleCommandService.removeRoleFromUser((long) request.getUserId(), (long) request.getRoleId())
                .map(apiResp -> Empty.getDefaultInstance())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private pb.role.RoleCommand.UserRoleResponse toProto(UserRoleResponse ur) {
        if (ur == null) {
            return pb.role.RoleCommand.UserRoleResponse.getDefaultInstance();
        }
        pb.role.RoleCommand.UserRoleResponse.Builder builder = pb.role.RoleCommand.UserRoleResponse.newBuilder();
        builder.setUserRoleId(0);
        if (ur.getUserId() != null) {
            builder.setUserId(ur.getUserId().intValue());
        }
        if (ur.getRoleId() != null) {
            builder.setRoleId(ur.getRoleId().intValue());
        }
        return builder.build();
    }
}
