package com.sanedge.transfer.handler;

import com.sanedge.transfer.domain.requests.FindAllTransfers;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.service.TransferQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transfer.MutinyTransferQueryServiceGrpc;
import pb.transfer.Transfer.ApiResponseTransfer;
import pb.transfer.Transfer.FindAllTransferRequest;
import pb.transfer.Transfer.FindByIdTransferRequest;
import pb.transfer.Transfer.FindTransferByTransferFromRequest;
import pb.transfer.Transfer.FindTransferByTransferToRequest;
import pb.transfer.TransferQuery.ApiResponsePaginationTransfer;
import pb.transfer.TransferQuery.ApiResponsePaginationTransferDeleteAt;
import pb.transfer.TransferQuery.ApiResponseTransfers;

@GrpcService
@Singleton
public class TransferQueryGrpcHandler extends MutinyTransferQueryServiceGrpc.TransferQueryServiceImplBase {

    @Inject
    TransferQueryService transferQueryService;

    @Override
    public Uni<ApiResponsePaginationTransfer> findAllTransfer(FindAllTransferRequest request) {
        FindAllTransfers domainReq = new FindAllTransfers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transferQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransfer.Builder builder = ApiResponsePaginationTransfer.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransferResponse r : apiResp.data()) {
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
    public Uni<ApiResponseTransfer> findByIdTransfer(FindByIdTransferRequest request) {
        return withSession(() -> transferQueryService.findById((long) request.getTransferId()))
                .map(apiResp -> {
                    ApiResponseTransfer.Builder builder = ApiResponseTransfer.newBuilder()
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
    public Uni<ApiResponseTransfers> findTransferByTransferFrom(FindTransferByTransferFromRequest request) {
        return withSession(() -> transferQueryService.findByTransferFrom(request.getTransferFrom()))
                .map(apiResp -> {
                    ApiResponseTransfers.Builder builder = ApiResponseTransfers.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransferResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseTransfers> findTransferByTransferTo(FindTransferByTransferToRequest request) {
        return withSession(() -> transferQueryService.findByTransferTo(request.getTransferTo()))
                .map(apiResp -> {
                    ApiResponseTransfers.Builder builder = ApiResponseTransfers.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransferResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationTransferDeleteAt> findByActiveTransfer(FindAllTransferRequest request) {
        FindAllTransfers domainReq = new FindAllTransfers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transferQueryService.findByActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransferDeleteAt.Builder builder = ApiResponsePaginationTransferDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransferResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationTransferDeleteAt> findByTrashedTransfer(FindAllTransferRequest request) {
        FindAllTransfers domainReq = new FindAllTransfers();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transferQueryService.findByTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransferDeleteAt.Builder builder = ApiResponsePaginationTransferDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransferResponseDeleteAt r : apiResp.data()) {
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

    private pb.transfer.Transfer.TransferResponse toProto(TransferResponse r) {
        if (r == null) {
            return pb.transfer.Transfer.TransferResponse.getDefaultInstance();
        }
        pb.transfer.Transfer.TransferResponse.Builder builder = pb.transfer.Transfer.TransferResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getTransferNo() != null) {
            builder.setTransferNo(r.getTransferNo());
        }
        if (r.getTransferFrom() != null) {
            builder.setTransferFrom(r.getTransferFrom());
        }
        if (r.getTransferTo() != null) {
            builder.setTransferTo(r.getTransferTo());
        }
        if (r.getTransferAmount() != null) {
            builder.setTransferAmount(r.getTransferAmount().intValue());
        }
        if (r.getTransferTime() != null) {
            builder.setTransferTime(r.getTransferTime().toString());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.transfer.Transfer.TransferResponseDeleteAt toProto(TransferResponseDeleteAt r) {
        if (r == null) {
            return pb.transfer.Transfer.TransferResponseDeleteAt.getDefaultInstance();
        }
        pb.transfer.Transfer.TransferResponseDeleteAt.Builder builder = pb.transfer.Transfer.TransferResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getTransferNo() != null) {
            builder.setTransferNo(r.getTransferNo());
        }
        if (r.getTransferFrom() != null) {
            builder.setTransferFrom(r.getTransferFrom());
        }
        if (r.getTransferTo() != null) {
            builder.setTransferTo(r.getTransferTo());
        }
        if (r.getTransferAmount() != null) {
            builder.setTransferAmount(r.getTransferAmount().intValue());
        }
        if (r.getTransferTime() != null) {
            builder.setTransferTime(r.getTransferTime().toString());
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