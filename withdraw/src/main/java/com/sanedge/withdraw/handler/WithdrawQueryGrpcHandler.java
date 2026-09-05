package com.sanedge.withdraw.handler;

import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.service.WithdrawQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.withdraw.MutinyWithdrawQueryServiceGrpc;
import pb.withdraw.Withdraw.ApiResponseWithdraw;
import pb.withdraw.Withdraw.ApiResponsesWithdraw;
import pb.withdraw.Withdraw.FindAllWithdrawByCardNumberRequest;
import pb.withdraw.Withdraw.FindAllWithdrawRequest;
import pb.withdraw.Withdraw.FindByIdWithdrawRequest;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdraw;
import pb.withdraw.WithdrawQuery.ApiResponsePaginationWithdrawDeleteAt;

@GrpcService
@Singleton
public class WithdrawQueryGrpcHandler extends MutinyWithdrawQueryServiceGrpc.WithdrawQueryServiceImplBase {

    @Inject
    WithdrawQueryService withdrawQueryService;

    @Override
    public Uni<ApiResponsePaginationWithdraw> findAllWithdraw(FindAllWithdrawRequest request) {
        FindAllWithdraws domainReq = new FindAllWithdraws();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> withdrawQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationWithdraw.Builder builder = ApiResponsePaginationWithdraw.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (WithdrawResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationWithdraw> findAllWithdrawByCardNumber(FindAllWithdrawByCardNumberRequest request) {
        FindAllWithdrawCardNumber domainReq = new FindAllWithdrawCardNumber();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> withdrawQueryService.findAllByCardNumber(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationWithdraw.Builder builder = ApiResponsePaginationWithdraw.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (WithdrawResponse r : apiResp.data()) {
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
    public Uni<ApiResponseWithdraw> findByIdWithdraw(FindByIdWithdrawRequest request) {
        return withSession(() -> withdrawQueryService.findById((long) request.getWithdrawId()))
                .map(apiResp -> {
                    ApiResponseWithdraw.Builder builder = ApiResponseWithdraw.newBuilder()
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
    public Uni<ApiResponsesWithdraw> findByCardNumber(pb.card.Card.FindByCardNumberRequest request) {
        return withSession(() -> withdrawQueryService.findByCard(request.getCardNumber()))
                .map(apiResp -> {
                    ApiResponsesWithdraw.Builder builder = ApiResponsesWithdraw.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (WithdrawResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationWithdrawDeleteAt> findByActive(FindAllWithdrawRequest request) {
        FindAllWithdraws domainReq = new FindAllWithdraws();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> withdrawQueryService.findByActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationWithdrawDeleteAt.Builder builder = ApiResponsePaginationWithdrawDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (WithdrawResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationWithdrawDeleteAt> findByTrashed(FindAllWithdrawRequest request) {
        FindAllWithdraws domainReq = new FindAllWithdraws();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> withdrawQueryService.findByTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationWithdrawDeleteAt.Builder builder = ApiResponsePaginationWithdrawDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (WithdrawResponseDeleteAt r : apiResp.data()) {
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

    private pb.withdraw.Withdraw.WithdrawResponse toProto(WithdrawResponse r) {
        if (r == null) {
            return pb.withdraw.Withdraw.WithdrawResponse.getDefaultInstance();
        }
        pb.withdraw.Withdraw.WithdrawResponse.Builder builder = pb.withdraw.Withdraw.WithdrawResponse.newBuilder();
        if (r.getId() != null) {
            builder.setWithdrawId(r.getId().intValue());
        }
        if (r.getWithdrawNo() != null) {
            builder.setWithdrawNo(r.getWithdrawNo());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getWithdrawAmount() != null) {
            builder.setWithdrawAmount(r.getWithdrawAmount().intValue());
        }
        if (r.getWithdrawTime() != null) {
            builder.setWithdrawTime(r.getWithdrawTime().toString());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.withdraw.Withdraw.WithdrawResponseDeleteAt toProto(WithdrawResponseDeleteAt r) {
        if (r == null) {
            return pb.withdraw.Withdraw.WithdrawResponseDeleteAt.getDefaultInstance();
        }
        pb.withdraw.Withdraw.WithdrawResponseDeleteAt.Builder builder = pb.withdraw.Withdraw.WithdrawResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setWithdrawId(r.getId().intValue());
        }
        if (r.getWithdrawNo() != null) {
            builder.setWithdrawNo(r.getWithdrawNo());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getWithdrawAmount() != null) {
            builder.setWithdrawAmount(r.getWithdrawAmount().intValue());
        }
        if (r.getWithdrawTime() != null) {
            builder.setWithdrawTime(r.getWithdrawTime().toString());
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

    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withSession(action);
    }
}