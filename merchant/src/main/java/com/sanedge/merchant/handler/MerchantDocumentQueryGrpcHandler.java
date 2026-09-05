package com.sanedge.merchant.handler;

import com.sanedge.merchant.service.MerchantDocumentQueryService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;

import pb.merchant_document.MutinyMerchantDocumentQueryServiceGrpc;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;

@GrpcService
@Singleton
public class MerchantDocumentQueryGrpcHandler extends MutinyMerchantDocumentQueryServiceGrpc.MerchantDocumentQueryServiceImplBase {

    @Inject
    MerchantDocumentQueryService merchantDocumentQueryService;

    @Override
    public Uni<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantDocumentQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocument.Builder builder = ApiResponsePaginationMerchantDocument.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponse r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantDocumentAt> findAllActive(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantDocumentQueryService.findAllActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocumentAt.Builder builder = ApiResponsePaginationMerchantDocumentAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest request) {
        com.sanedge.merchant.domain.requests.FindAllMerchantDocuments domainReq = new com.sanedge.merchant.domain.requests.FindAllMerchantDocuments();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> merchantDocumentQueryService.findAllTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationMerchantDocumentAt.Builder builder = ApiResponsePaginationMerchantDocumentAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (MerchantDocumentResponseDeleteAt r : apiResp.data()) {
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
    public Uni<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest request) {
        return withSession(() -> merchantDocumentQueryService.findById((long) request.getDocumentId()))
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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

    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withSession(action);
    }

    private pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument toProto(MerchantDocumentResponse r) {
        if (r == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.Builder builder = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocument.newBuilder();
        if (r.getDocumentId() != null) {
            builder.setDocumentId(r.getDocumentId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getDocumentType() != null) {
            builder.setDocumentType(r.getDocumentType());
        }
        if (r.getDocumentUrl() != null) {
            builder.setDocumentUrl(r.getDocumentUrl());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getNote() != null) {
            builder.setNote(r.getNote());
        }
        if (r.getCreatedAt() != null) {
            builder.setUploadedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt toProto(MerchantDocumentResponseDeleteAt r) {
        if (r == null) {
            return pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.getDefaultInstance();
        }
        pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.Builder builder = pb.merchant_document.MerchantDocumentOuterClass.MerchantDocumentDeleteAt.newBuilder();
        if (r.getDocumentId() != null) {
            builder.setDocumentId(r.getDocumentId().intValue());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId().intValue());
        }
        if (r.getDocumentType() != null) {
            builder.setDocumentType(r.getDocumentType());
        }
        if (r.getDocumentUrl() != null) {
            builder.setDocumentUrl(r.getDocumentUrl());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getNote() != null) {
            builder.setNote(r.getNote());
        }
        if (r.getCreatedAt() != null) {
            builder.setUploadedAt(r.getCreatedAt());
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
