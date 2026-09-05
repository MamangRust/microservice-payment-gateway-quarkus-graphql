package com.sanedge.merchant.handler;

import com.sanedge.merchant.domain.response.MerchantResponse;
import com.sanedge.merchant.domain.response.MerchantResponseDeleteAt;
import com.sanedge.merchant.service.MerchantQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.ApiResponsesMerchant;
import pb.merchant.Merchant.FindAllMerchantRequest;
import pb.merchant.Merchant.FindByApiKeyRequest;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.Merchant.FindByMerchantUserIdRequest;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchant;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt;
import pb.merchant.MutinyMerchantQueryServiceGrpc;

@GrpcService
@Singleton
public class MerchantQueryGrpcHandler extends MutinyMerchantQueryServiceGrpc.MerchantQueryServiceImplBase {

    @Inject
    MerchantQueryService merchantQueryService;

    @Override
    public Uni<ApiResponsePaginationMerchant> findAllMerchant(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchant.Builder builder = ApiResponsePaginationMerchant.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponse mr : apiResp.data()) {
                            builder.addData(toProto(mr));
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
    public Uni<ApiResponseMerchant> findByIdMerchant(FindByIdMerchantRequest request) {
        return withSession(() -> merchantQueryService.findById((long) request.getMerchantId()))
                .map(apiResp -> {
                    ApiResponseMerchant.Builder builder = ApiResponseMerchant.newBuilder()
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
    public Uni<ApiResponseMerchant> findByApiKey(FindByApiKeyRequest request) {
        return withSession(() -> merchantQueryService.findByApiKey(request.getApiKey()))
                .map(apiResp -> {
                    ApiResponseMerchant.Builder builder = ApiResponseMerchant.newBuilder()
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
    public Uni<ApiResponsesMerchant> findByMerchantUserId(FindByMerchantUserIdRequest request) {
        return withSession(() -> merchantQueryService.findByUserId((long) request.getUserId()))
                .map(apiResp -> {
                    ApiResponsesMerchant.Builder builder = ApiResponsesMerchant.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponse mr : apiResp.data()) {
                            builder.addData(toProto(mr));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantQueryService.findByActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDeleteAt.Builder builder = ApiResponsePaginationMerchantDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponseDeleteAt mrd : apiResp.data()) {
                            builder.addData(toProto(mrd));
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
    public Uni<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchants domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchants();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantQueryService.findByTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDeleteAt.Builder builder = ApiResponsePaginationMerchantDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantResponseDeleteAt mrd : apiResp.data()) {
                            builder.addData(toProto(mrd));
                        }
                    }
                    if (apiResp.pagination() != null) {
                        builder.setPaginationMeta(toProto(apiResp.pagination()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withSession(action);
    }

    private pb.merchant.Merchant.MerchantResponse toProto(MerchantResponse r) {
        if (r == null) {
            return pb.merchant.Merchant.MerchantResponse.getDefaultInstance();
        }
        pb.merchant.Merchant.MerchantResponse.Builder builder = pb.merchant.Merchant.MerchantResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getApiKey() != null) {
            builder.setApiKey(r.getApiKey());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant.Merchant.MerchantResponseDeleteAt toProto(MerchantResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant.Merchant.MerchantResponseDeleteAt.getDefaultInstance();
        }
        pb.merchant.Merchant.MerchantResponseDeleteAt.Builder builder = pb.merchant.Merchant.MerchantResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getName() != null) {
            builder.setName(r.getName());
        }
        if (r.getApiKey() != null) {
            builder.setApiKey(r.getApiKey());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
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
