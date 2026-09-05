package com.sanedge.merchant.handler;

import com.sanedge.merchant.service.MerchantDocumentCommandService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;

import pb.merchant_document.MutinyMerchantDocumentCommandServiceGrpc;
import pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest;
import pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import com.google.protobuf.Empty;

@GrpcService
@Singleton
public class MerchantDocumentCommandGrpcHandler extends MutinyMerchantDocumentCommandServiceGrpc.MerchantDocumentCommandServiceImplBase {

    @Inject
    MerchantDocumentCommandService merchantDocumentCommandService;

    @Override
    public Uni<ApiResponseMerchantDocument> create(CreateMerchantDocumentRequest request) {
        com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest domainReq = new com.sanedge.merchant.domain.requests.CreateMerchantDocumentRequest();
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setDocumentType(request.getDocumentType());
        domainReq.setDocumentUrl(request.getDocumentUrl());

        return merchantDocumentCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> update(UpdateMerchantDocumentRequest request) {
        com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest domainReq = new com.sanedge.merchant.domain.requests.UpdateMerchantDocumentRequest();
        domainReq.setDocumentId((long) request.getDocumentId());
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setDocumentType(request.getDocumentType());
        domainReq.setDocumentUrl(request.getDocumentUrl());
        domainReq.setNote(request.getNote());
        domainReq.setStatus(request.getStatus());

        return merchantDocumentCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocument> updateStatus(UpdateMerchantDocumentStatusRequest request) {
        com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus domainReq = new com.sanedge.merchant.domain.requests.UpdateMerchantDocumentStatus();
        domainReq.setDocumentId((long) request.getDocumentId());
        domainReq.setMerchantId((long) request.getMerchantId());
        domainReq.setNote(request.getNote());
        domainReq.setStatus(request.getStatus());

        return merchantDocumentCommandService.updateStatus(domainReq)
                .map(apiResp -> {
                    ApiResponseMerchantDocument.Builder builder = ApiResponseMerchantDocument.newBuilder()
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
    public Uni<ApiResponseMerchantDocumentDeleteAt> trashed(FindMerchantDocumentByIdRequest request) {
        return merchantDocumentCommandService.trash((long) request.getDocumentId())
                .map(apiResp -> {
                    ApiResponseMerchantDocumentDeleteAt.Builder builder = ApiResponseMerchantDocumentDeleteAt.newBuilder()
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
    public Uni<ApiResponseMerchantDocumentDeleteAt> restore(FindMerchantDocumentByIdRequest request) {
        return merchantDocumentCommandService.restore((long) request.getDocumentId())
                .map(apiResp -> {
                    ApiResponseMerchantDocumentDeleteAt.Builder builder = ApiResponseMerchantDocumentDeleteAt.newBuilder()
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
    public Uni<ApiResponseMerchantDocumentDelete> deletePermanent(FindMerchantDocumentByIdRequest request) {
        return merchantDocumentCommandService.deletePermanent((long) request.getDocumentId())
                .map(apiResp -> ApiResponseMerchantDocumentDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantDocumentAll> restoreAll(Empty request) {
        return merchantDocumentCommandService.restoreAll()
                .map(apiResp -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseMerchantDocumentAll> deleteAllPermanent(Empty request) {
        return merchantDocumentCommandService.deleteAllPermanent()
                .map(apiResp -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
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
}
