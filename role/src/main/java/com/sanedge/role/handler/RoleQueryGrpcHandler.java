package com.sanedge.role.handler;

import java.util.List;

import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.service.RoleQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.role.MutinyRoleServiceGrpc;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponsesRole;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.RoleQuery.ApiResponsePaginationRole;
import pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt;

@GrpcService
@Singleton
public class RoleQueryGrpcHandler extends MutinyRoleServiceGrpc.RoleServiceImplBase {

    @Inject
    RoleQueryService roleQueryService;

    @Override
    public Uni<ApiResponsePaginationRole> findAllRole(FindAllRoleRequest request) {
        FindAllRoles domainReq = new FindAllRoles();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return roleQueryService.findAllPaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationRole.Builder builder = ApiResponsePaginationRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (RoleResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<pb.role.Role.ApiResponseRole> findByIdRole(FindByIdRoleRequest request) {
        return roleQueryService.findById((long) request.getRoleId())
                .map(apiResp -> {
                    ApiResponseRole.Builder builder = ApiResponseRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof jakarta.ws.rs.NotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
                });
    }

    @Override
    public Uni<pb.role.Role.ApiResponseRole> findByNameRole(pb.role.RoleQuery.FindByNameRoleRequest request) {
        return roleQueryService.findByName(request.getName())
                .map(apiResp -> {
                    ApiResponseRole.Builder builder = ApiResponseRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof jakarta.ws.rs.NotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
                });
    }

    @Override
    public Uni<ApiResponsePaginationRoleDeleteAt> findByActive(FindAllRoleRequest request) {
        FindAllRoles domainReq = new FindAllRoles();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return roleQueryService.findActivePaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationRoleDeleteAt.Builder builder = ApiResponsePaginationRoleDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (RoleResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationRoleDeleteAt> findByTrashed(FindAllRoleRequest request) {
        FindAllRoles domainReq = new FindAllRoles();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return roleQueryService.findTrashedPaginated(domainReq)
                .map(apiResp -> {
                    ApiResponsePaginationRoleDeleteAt.Builder builder = ApiResponsePaginationRoleDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (RoleResponseDeleteAt r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsesRole> findByUserId(FindByIdUserRoleRequest request) {
        return roleQueryService.findByUserId((long) request.getUserId())
                .map(apiResp -> {
                    ApiResponsesRole.Builder builder = ApiResponsesRole.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (RoleResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    return builder.build();
                })
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

    private pb.common.PaginationMeta toProto(com.sanedge.common.domain.response.PaginationMeta m) {
        if (m == null) {
            return pb.common.PaginationMeta.getDefaultInstance();
        }
        return pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(m.currentPage())
                .setPageSize(m.pageSize())
                .setTotalPages(m.totalPages())
                .setTotalRecords(m.totalRecords())
                .build();
    }
}
