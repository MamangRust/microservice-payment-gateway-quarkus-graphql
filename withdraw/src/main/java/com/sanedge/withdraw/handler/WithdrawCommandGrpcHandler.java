package com.sanedge.withdraw.handler;

import com.google.protobuf.Empty;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.service.WithdrawCommandService;

import com.sanedge.common.grpc.GrpcStatusMapper;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.withdraw.MutinyWithdrawCommandServiceGrpc;
import pb.withdraw.Withdraw.ApiResponseWithdraw;
import pb.withdraw.Withdraw.ApIResponseWithdrawDeleteAt;
import pb.withdraw.Withdraw.FindByIdWithdrawRequest;
import pb.withdraw.WithdrawCommand.ApiResponseWithdrawAll;
import pb.withdraw.WithdrawCommand.ApiResponseWithdrawDelete;
import pb.withdraw.WithdrawCommand.CreateWithdrawRequest;
import pb.withdraw.WithdrawCommand.UpdateWithdrawRequest;

@GrpcService
@Singleton
public class WithdrawCommandGrpcHandler extends MutinyWithdrawCommandServiceGrpc.WithdrawCommandServiceImplBase {

    @Inject
    WithdrawCommandService withdrawCommandService;

    @Override
    public Uni<ApiResponseWithdraw> createWithdraw(CreateWithdrawRequest request) {
        com.sanedge.withdraw.domain.requests.CreateWithdrawRequest domainReq = new com.sanedge.withdraw.domain.requests.CreateWithdrawRequest();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setWithdrawAmount((long) request.getWithdrawAmount());
        domainReq.setIdempotencyKey(request.getIdempotencyKey().isBlank() ? null : request.getIdempotencyKey());

        if (request.hasWithdrawTime()) {
            domainReq.setWithdrawTime(java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(request.getWithdrawTime().getSeconds()),
                java.time.ZoneId.systemDefault()
            ));
        }

        return withdrawCommandService.create(domainReq)
                .map(apiResp -> {
                    ApiResponseWithdraw.Builder builder = ApiResponseWithdraw.newBuilder()
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
    public Uni<ApiResponseWithdraw> updateWithdraw(UpdateWithdrawRequest request) {
        com.sanedge.withdraw.domain.requests.UpdateWithdrawRequest domainReq = new com.sanedge.withdraw.domain.requests.UpdateWithdrawRequest();
        domainReq.setWithdrawId((long) request.getWithdrawId());
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setWithdrawAmount((long) request.getWithdrawAmount());

        if (request.hasWithdrawTime()) {
            domainReq.setWithdrawTime(java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(request.getWithdrawTime().getSeconds()),
                java.time.ZoneId.systemDefault()
            ));
        }

        return withdrawCommandService.update(domainReq)
                .map(apiResp -> {
                    ApiResponseWithdraw.Builder builder = ApiResponseWithdraw.newBuilder()
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
    public Uni<ApIResponseWithdrawDeleteAt> trashedWithdraw(FindByIdWithdrawRequest request) {
        return withdrawCommandService.trashed((long) request.getWithdrawId())
                .map(apiResp -> {
                    ApIResponseWithdrawDeleteAt.Builder builder = ApIResponseWithdrawDeleteAt.newBuilder()
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
    public Uni<ApIResponseWithdrawDeleteAt> restoreWithdraw(FindByIdWithdrawRequest request) {
        return withdrawCommandService.restore((long) request.getWithdrawId())
                .map(apiResp -> {
                    ApIResponseWithdrawDeleteAt.Builder builder = ApIResponseWithdrawDeleteAt.newBuilder()
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
    public Uni<ApiResponseWithdrawDelete> deleteWithdrawPermanent(FindByIdWithdrawRequest request) {
        return withdrawCommandService.deletePermanent((long) request.getWithdrawId())
                .map(apiResp -> ApiResponseWithdrawDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseWithdrawAll> restoreAllWithdraw(Empty request) {
        return withdrawCommandService.restoreAll()
                .map(apiResp -> ApiResponseWithdrawAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
    }

    @Override
    public Uni<ApiResponseWithdrawAll> deleteAllWithdrawPermanent(Empty request) {
        return withdrawCommandService.deleteAll()
                .map(apiResp -> ApiResponseWithdrawAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> GrpcStatusMapper.toStatusRuntimeException(e));
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
        pb.withdraw.Withdraw.WithdrawResponseDeleteAt.Builder builder = pb.withdraw.Withdraw.WithdrawResponseDeleteAt.newBuilder();
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
}
