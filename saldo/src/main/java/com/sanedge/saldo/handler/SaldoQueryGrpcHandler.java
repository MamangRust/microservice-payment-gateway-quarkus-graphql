package com.sanedge.saldo.handler;

import com.sanedge.saldo.domain.requests.FindAllSaldos;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.service.SaldoQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.saldo.MutinySaldoQueryServiceGrpc;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.FindAllSaldoRequest;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldo;
import pb.saldo.SaldoQuery.ApiResponsePaginationSaldoDeleteAt;

@GrpcService
@Singleton
public class SaldoQueryGrpcHandler extends MutinySaldoQueryServiceGrpc.SaldoQueryServiceImplBase {

    @Inject
    SaldoQueryService saldoQueryService;

    @Override
    public Uni<ApiResponsePaginationSaldo> findAllSaldo(FindAllSaldoRequest request) {
        FindAllSaldos domainReq = new FindAllSaldos();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> saldoQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationSaldo.Builder builder = ApiResponsePaginationSaldo.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SaldoResponse r : apiResp.data()) {
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
    public Uni<ApiResponseSaldo> findByIdSaldo(FindByIdSaldoRequest request) {
        return withSession(() -> saldoQueryService.findById((long) request.getSaldoId()))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponseSaldo> findByCardNumber(pb.card.Card.FindByCardNumberRequest request) {
        return withSession(() -> saldoQueryService.findByCard(request.getCardNumber()))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponsePaginationSaldoDeleteAt> findByActive(FindAllSaldoRequest request) {
        FindAllSaldos domainReq = new FindAllSaldos();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> saldoQueryService.findActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationSaldoDeleteAt.Builder builder = ApiResponsePaginationSaldoDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SaldoResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationSaldoDeleteAt> findByTrashed(FindAllSaldoRequest request) {
        FindAllSaldos domainReq = new FindAllSaldos();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> saldoQueryService.findTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationSaldoDeleteAt.Builder builder = ApiResponsePaginationSaldoDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (SaldoResponseDeleteAt r : apiResp.data()) {
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

    private pb.saldo.Saldo.SaldoResponse toProto(SaldoResponse r) {
        if (r == null) {
            return pb.saldo.Saldo.SaldoResponse.getDefaultInstance();
        }
        pb.saldo.Saldo.SaldoResponse.Builder builder = pb.saldo.Saldo.SaldoResponse.newBuilder();
        if (r.getId() != null) {
            builder.setSaldoId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTotalBalance() != null) {
            builder.setTotalBalance(r.getTotalBalance().intValue());
        }
        if (r.getWithdrawTime() != null) {
            builder.setWithdrawTime(r.getWithdrawTime().toString());
        }
        if (r.getWithdrawAmount() != null) {
            builder.setWithdrawAmount(r.getWithdrawAmount().intValue());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.saldo.Saldo.SaldoResponseDeleteAt toProto(SaldoResponseDeleteAt r) {
        if (r == null) {
            return pb.saldo.Saldo.SaldoResponseDeleteAt.getDefaultInstance();
        }
        pb.saldo.Saldo.SaldoResponseDeleteAt.Builder builder = pb.saldo.Saldo.SaldoResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setSaldoId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTotalBalance() != null) {
            builder.setTotalBalance(r.getTotalBalance().intValue());
        }
        if (r.getWithdrawTime() != null) {
            builder.setWithdrawTime(r.getWithdrawTime().toString());
        }
        if (r.getWithdrawAmount() != null) {
            builder.setWithdrawAmount(r.getWithdrawAmount().intValue());
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
