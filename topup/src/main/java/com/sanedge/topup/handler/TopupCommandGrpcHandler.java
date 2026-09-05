package com.sanedge.topup.handler;

import com.google.protobuf.Empty;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.service.TopupCommandService;

import com.sanedge.common.grpc.GrpcStatusMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.topup.MutinyTopupCommandServiceGrpc;
import pb.topup.Topup.ApiResponseTopup;
import pb.topup.Topup.ApiResponseTopupDeleteAt;
import pb.topup.Topup.FindByIdTopupRequest;
import pb.topup.TopupCommand.ApiResponseTopupAll;
import pb.topup.TopupCommand.ApiResponseTopupDelete;
import pb.topup.TopupCommand.CreateTopupRequest;
import pb.topup.TopupCommand.UpdateTopupRequest;

@GrpcService
@Singleton
public class TopupCommandGrpcHandler extends MutinyTopupCommandServiceGrpc.TopupCommandServiceImplBase {

    @Inject
    TopupCommandService topupCommandService;

    @Override
    public Uni<ApiResponseTopup> createTopup(CreateTopupRequest request) {
        com.sanedge.topup.domain.requests.CreateTopupRequest domainReq = new com.sanedge.topup.domain.requests.CreateTopupRequest();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTopupAmount((long) request.getTopupAmount());
        domainReq.setTopupMethod(request.getTopupMethod());
        domainReq.setIdempotencyKey(request.getIdempotencyKey().isBlank() ? null : request.getIdempotencyKey());

        return topupCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseTopup.Builder builder = ApiResponseTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopup> updateTopup(UpdateTopupRequest request) {
        com.sanedge.topup.domain.requests.UpdateTopupRequest domainReq = new com.sanedge.topup.domain.requests.UpdateTopupRequest();
        domainReq.setTopupId((long) request.getTopupId());
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTopupAmount((long) request.getTopupAmount());
        domainReq.setTopupMethod(request.getTopupMethod());

        return topupCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseTopup.Builder builder = ApiResponseTopup.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopupDeleteAt> trashedTopup(FindByIdTopupRequest request) {
        return topupCommandService.trashed((long) request.getTopupId())
                .map(apiResp -> {
                    ApiResponseTopupDeleteAt.Builder builder = ApiResponseTopupDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopupDeleteAt> restoreTopup(FindByIdTopupRequest request) {
        return topupCommandService.restore((long) request.getTopupId())
                .map(apiResp -> {
                    ApiResponseTopupDeleteAt.Builder builder = ApiResponseTopupDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopupDelete> deleteTopupPermanent(FindByIdTopupRequest request) {
        return topupCommandService.deletePermanent((long) request.getTopupId())
                .map(apiResp -> ApiResponseTopupDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopupAll> restoreAllTopup(Empty request) {
        return topupCommandService.restoreAll()
                .map(apiResp -> ApiResponseTopupAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseTopupAll> deleteAllTopupPermanent(Empty request) {
        return topupCommandService.deleteAll()
                .map(apiResp -> ApiResponseTopupAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
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
}
