package com.sanedge.transaction.handler;

import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.service.TransactionQueryService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.transaction.MutinyTransactionQueryServiceGrpc;
import pb.transaction.Transaction.ApiResponseTransaction;
import pb.transaction.Transaction.ApiResponseTransactions;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionQuery.ApiResponsePaginationTransaction;
import pb.transaction.TransactionQuery.ApiResponsePaginationTransactionDeleteAt;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;
import pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest;

@GrpcService
@Singleton
public class TransactionQueryGrpcHandler extends MutinyTransactionQueryServiceGrpc.TransactionQueryServiceImplBase {

    @Inject
    TransactionQueryService transactionQueryService;

    @Override
    public Uni<ApiResponsePaginationTransaction> findAllTransaction(FindAllTransactionRequest request) {
        FindAllTransactions domainReq = new FindAllTransactions();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transactionQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransactionResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationTransaction> findAllTransactionByCardNumber(
            FindAllTransactionCardNumberRequest request) {
        FindAllTransactionCardNumber domainReq = new FindAllTransactionCardNumber();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transactionQueryService.findAllByCardNumber(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransaction.Builder builder = ApiResponsePaginationTransaction.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransactionResponse r : apiResp.data()) {
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
    public Uni<ApiResponseTransaction> findByIdTransaction(FindByIdTransactionRequest request) {
        return withSession(() -> transactionQueryService.findById((long) request.getTransactionId()))
                .map(apiResp -> {
                    ApiResponseTransaction.Builder builder = ApiResponseTransaction.newBuilder()
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
    public Uni<ApiResponseTransactions> findTransactionByMerchantId(FindTransactionByMerchantIdRequest request) {
        return withSession(() -> transactionQueryService.findByMerchantId((long) request.getMerchantId()))
                .map(apiResp -> {
                    ApiResponseTransactions.Builder builder = ApiResponseTransactions.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransactionResponse r : apiResp.data()) {
                            builder.addData(toProto(r));
                        }
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponsePaginationTransactionDeleteAt> findByActiveTransaction(FindAllTransactionRequest request) {
        FindAllTransactions domainReq = new FindAllTransactions();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transactionQueryService.findByActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransactionDeleteAt.Builder builder = ApiResponsePaginationTransactionDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransactionResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationTransactionDeleteAt> findByTrashedTransaction(FindAllTransactionRequest request) {
        FindAllTransactions domainReq = new FindAllTransactions();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> transactionQueryService.findByTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationTransactionDeleteAt.Builder builder = ApiResponsePaginationTransactionDeleteAt
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (TransactionResponseDeleteAt r : apiResp.data()) {
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

    private pb.transaction.Transaction.TransactionResponse toProto(TransactionResponse r) {
        if (r == null) {
            return pb.transaction.Transaction.TransactionResponse.getDefaultInstance();
        }
        pb.transaction.Transaction.TransactionResponse.Builder builder = pb.transaction.Transaction.TransactionResponse
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTransactionNo() != null) {
            builder.setTransactionNo(r.getTransactionNo());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount().intValue());
        }
        if (r.getPaymentMethod() != null) {
            builder.setPaymentMethod(r.getPaymentMethod());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getTransactionTime() != null) {
            builder.setTransactionTime(r.getTransactionTime().toString());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt().toString());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt().toString());
        }
        return builder.build();
    }

    private pb.transaction.Transaction.TransactionResponseDeleteAt toProto(TransactionResponseDeleteAt r) {
        if (r == null) {
            return pb.transaction.Transaction.TransactionResponseDeleteAt.getDefaultInstance();
        }
        pb.transaction.Transaction.TransactionResponseDeleteAt.Builder builder = pb.transaction.Transaction.TransactionResponseDeleteAt
                .newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getTransactionNo() != null) {
            builder.setTransactionNo(r.getTransactionNo());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount().intValue());
        }
        if (r.getPaymentMethod() != null) {
            builder.setPaymentMethod(r.getPaymentMethod());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getTransactionTime() != null) {
            builder.setTransactionTime(r.getTransactionTime().toString());
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