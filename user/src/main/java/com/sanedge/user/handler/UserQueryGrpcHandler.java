package com.sanedge.user.handler;

import java.util.stream.Collectors;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.user.service.UserQueryService;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import pb.user.MutinyUserQueryServiceGrpc;
import pb.user.User.ApiResponseUser;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.User.UserResponseDeleteAt;
import pb.user.UserQuery.ApiResponsePaginationUser;
import pb.user.UserQuery.ApiResponsePaginationUserDeleteAt;

@GrpcService
public class UserQueryGrpcHandler extends MutinyUserQueryServiceGrpc.UserQueryServiceImplBase {

    @Inject
    UserQueryService userQueryService;

    @Override
    public Uni<ApiResponsePaginationUser> findAll(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findAllPaginated(req)
                .map(res -> {
                    ApiResponsePaginationUser.Builder builder = ApiResponsePaginationUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());

                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponse)
                                .collect(Collectors.toList()));
                    }

                    if (res.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(res.pagination().currentPage())
                                .setPageSize(res.pagination().pageSize())
                                .setTotalPages(res.pagination().totalPages())
                                .setTotalRecords(res.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponseUser> findById(FindByIdUserRequest request) {
        return userQueryService.findById((long) request.getId())
                .map(res -> {
                    ApiResponseUser.Builder builder = ApiResponseUser.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.setData(mapToUserResponse(res.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponsePaginationUserDeleteAt> findByActive(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findActivePaginated(req)
                .map(res -> {
                    ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponseDeleteAt)
                                .collect(Collectors.toList()));
                    }
                    if (res.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(res.pagination().currentPage())
                                .setPageSize(res.pagination().pageSize())
                                .setTotalPages(res.pagination().totalPages())
                                .setTotalRecords(res.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                });
    }

    @Override
    public Uni<ApiResponsePaginationUserDeleteAt> findByTrashed(FindAllUserRequest request) {
        FindAllUsers req = new FindAllUsers();
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());
        req.setSearch(request.getSearch());

        return userQueryService.findTrashedPaginated(req)
                .map(res -> {
                    ApiResponsePaginationUserDeleteAt.Builder builder = ApiResponsePaginationUserDeleteAt.newBuilder()
                            .setStatus(res.status())
                            .setMessage(res.message());
                    if (res.data() != null) {
                        builder.addAllData(res.data().stream()
                                .map(this::mapToUserResponseDeleteAt)
                                .collect(Collectors.toList()));
                    }
                    if (res.pagination() != null) {
                        builder.setPaginationMeta(pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(res.pagination().currentPage())
                                .setPageSize(res.pagination().pageSize())
                                .setTotalPages(res.pagination().totalPages())
                                .setTotalRecords(res.pagination().totalRecords())
                                .build());
                    }
                    return builder.build();
                });
    }

    private UserResponse mapToUserResponse(com.sanedge.user.domain.response.UserResponse u) {
        return UserResponse.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "")
                .build();
    }

    private UserResponseDeleteAt mapToUserResponseDeleteAt(com.sanedge.user.domain.response.UserResponseDeleteAt u) {
        UserResponseDeleteAt.Builder builder = UserResponseDeleteAt.newBuilder()
                .setId(u.getId().intValue())
                .setFirstname(u.getFirstname())
                .setLastname(u.getLastname())
                .setEmail(u.getEmail())
                .setCreatedAt(u.getCreatedAt() != null ? u.getCreatedAt().toString() : "")
                .setUpdatedAt(u.getUpdatedAt() != null ? u.getUpdatedAt().toString() : "");
        if (u.getDeletedAt() != null) {
            builder.setDeletedAt(
                    com.google.protobuf.StringValue.newBuilder().setValue(u.getDeletedAt().toString()).build());
        }
        return builder.build();
    }
}
