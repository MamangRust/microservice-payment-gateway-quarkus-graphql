package com.sanedge.topup.handler;

import com.sanedge.topup.domain.requests.FindAllTopups;
import com.sanedge.topup.domain.requests.FindAllTopupsByCardNumber;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.service.TopupQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.topup.MutinyTopupQueryServiceGrpc;
import pb.topup.Topup.ApiResponseTopup;
import pb.topup.Topup.FindByCardNumberTopupRequest;
import pb.topup.Topup.FindByIdTopupRequest;
import pb.topup.TopupQuery.ApiResponsePaginationTopup;
import pb.topup.TopupQuery.ApiResponsePaginationTopupDeleteAt;
import pb.topup.TopupQuery.FindAllTopupByCardNumberRequest;
import pb.topup.TopupQuery.FindAllTopupRequest;

@GrpcService
@Singleton
public class TopupQueryGrpcHandler extends MutinyTopupQueryServiceGrpc.TopupQueryServiceImplBase {

    @Inject
    TopupQueryService topupQueryService;

    @Override
    public Uni<ApiResponsePaginationTopup> findAllTopup(FindAllTopupRequest request) {
        FindAllTopups domainReq = new FindAllTopups();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> topupQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTopup.Builder builder = ApiResponsePaginationTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TopupResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationTopup> findAllTopupByCardNumber(FindAllTopupByCardNumberRequest request) {
        FindAllTopupsByCardNumber domainReq = new FindAllTopupsByCardNumber();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> topupQueryService.findAllByCardNumber(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTopup.Builder builder = ApiResponsePaginationTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TopupResponse r : apiResp.data()) {
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
    public Uni<ApiResponseTopup> findByIdTopup(FindByIdTopupRequest request) {
        return withSession(() -> topupQueryService.findById((long) request.getTopupId()))
                .map(apiResp -> {
                    ApiResponseTopup.Builder builder = ApiResponseTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> {
                    if (e instanceof com.sanedge.common.exception.ResourceNotFoundException) {
                        return Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException();
                    }
                    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
                });
    }

    @Override
    public Uni<ApiResponseTopup> findByCardNumberTopup(FindByCardNumberTopupRequest request) {
        return withSession(() -> topupQueryService.findByCard(request.getCardNumber()))
                .map(apiResp -> {
                    ApiResponseTopup.Builder builder = ApiResponseTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null && !apiResp.data().isEmpty()) {
                        builder.setData(toProto(apiResp.data().get(0)));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationTopupDeleteAt> findByActive(FindAllTopupRequest request) {
        FindAllTopups domainReq = new FindAllTopups();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> topupQueryService.findActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTopupDeleteAt.Builder builder = ApiResponsePaginationTopupDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TopupResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationTopupDeleteAt> findByTrashed(FindAllTopupRequest request) {
        FindAllTopups domainReq = new FindAllTopups();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> topupQueryService.findTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTopupDeleteAt.Builder builder = ApiResponsePaginationTopupDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TopupResponseDeleteAt r : apiResp.data()) {
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

    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withSession(action);
    }

    private pb.topup.Topup.TopupResponse toProto(TopupResponse r) {
        if (r == null) {
            return pb.topup.Topup.TopupResponse.getDefaultInstance();
        }
        pb.topup.Topup.TopupResponse.Builder builder = pb.topup.Topup.TopupResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTopupNo() != null) {
            builder.setTopupNo(r.getTopupNo());
        }
        if (r.getTopupAmount() != null) {
            builder.setTopupAmount(r.getTopupAmount().intValue());
        }
        if (r.getTopupMethod() != null) {
            builder.setTopupMethod(r.getTopupMethod());
        }
        if (r.getTopupTime() != null) {
            builder.setTopupTime(r.getTopupTime().toString());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.topup.Topup.TopupResponseDeleteAt toProto(TopupResponseDeleteAt r) {
        if (r == null) {
            return pb.topup.Topup.TopupResponseDeleteAt.getDefaultInstance();
        }
        pb.topup.Topup.TopupResponseDeleteAt.Builder builder = pb.topup.Topup.TopupResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTopupNo() != null) {
            builder.setTopupNo(r.getTopupNo());
        }
        if (r.getTopupAmount() != null) {
            builder.setTopupAmount(r.getTopupAmount().intValue());
        }
        if (r.getTopupMethod() != null) {
            builder.setTopupMethod(r.getTopupMethod());
        }
        if (r.getTopupTime() != null) {
            builder.setTopupTime(r.getTopupTime().toString());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        if (r.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(r.getDeletedAt().toString()));
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
