package com.sanedge.saldo.handler;

import com.google.protobuf.Empty;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.service.SaldoCommandService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.saldo.MutinySaldoCommandServiceGrpc;
import pb.saldo.Saldo.ApiResponseSaldo;
import pb.saldo.Saldo.ApiResponseSaldoDeleteAt;
import pb.saldo.Saldo.FindByIdSaldoRequest;
import pb.saldo.SaldoCommand.ApiResponseSaldoAll;
import pb.saldo.SaldoCommand.ApiResponseSaldoDelete;
import pb.saldo.SaldoCommand.CreateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest;
import pb.saldo.SaldoCommand.UpdateSaldoRequest;
import pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest;

@GrpcService
@Singleton
public class SaldoCommandGrpcHandler extends MutinySaldoCommandServiceGrpc.SaldoCommandServiceImplBase {

    @Inject
    SaldoCommandService saldoCommandService;

    @Override
    public Uni<ApiResponseSaldo> createSaldo(CreateSaldoRequest request) {
        markCurrentContextSafe();
        com.sanedge.saldo.domain.requests.CreateSaldoRequest domainReq = new com.sanedge.saldo.domain.requests.CreateSaldoRequest();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTotalBalance((long) request.getTotalBalance());

        return withTransaction(() -> saldoCommandService.create(domainReq))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponseSaldo> updateSaldo(UpdateSaldoRequest request) {
        markCurrentContextSafe();
        com.sanedge.saldo.domain.requests.UpdateSaldoRequest domainReq = new com.sanedge.saldo.domain.requests.UpdateSaldoRequest();
        domainReq.setSaldoId((long) request.getSaldoId());
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTotalBalance((long) request.getTotalBalance());

        return withTransaction(() -> saldoCommandService.update(domainReq))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponseSaldo> updateSaldoBalance(UpdateSaldoBalanceRequest request) {
        markCurrentContextSafe();
        com.sanedge.saldo.domain.requests.UpdateSaldoBalance domainReq = new com.sanedge.saldo.domain.requests.UpdateSaldoBalance();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTotalBalance((long) request.getTotalBalance());
        if (request.hasDeltaBalance()) {
            domainReq.setDeltaBalance((long) request.getDeltaBalance());
            domainReq.setMinimumBalance(request.hasMinimumBalance() ? (long) request.getMinimumBalance() : 0L);
        }
        if (request.hasOperationKey()) {
            domainReq.setOperationKey(request.getOperationKey());
        }

        return withTransaction(() -> saldoCommandService.updateSaldoBalance(domainReq))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponseSaldo> updateSaldoWithdraw(UpdateSaldoWithdrawRequest request) {
        markCurrentContextSafe();
        com.sanedge.saldo.domain.requests.UpdateSaldoWithdraw domainReq = new com.sanedge.saldo.domain.requests.UpdateSaldoWithdraw();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setTotalBalance((long) request.getTotalBalance());
        domainReq.setWithdrawAmount((long) request.getWithdrawAmount());
        if (request.getWithdrawTime() != null && !request.getWithdrawTime().isEmpty()) {
            // Withdraw service sends java.sql.Timestamp#toString() ("2026-08-10 06:41:22.0"),
            // while LocalDateTime#parse requires ISO-8601 ("T" separator). Normalize both.
            domainReq.setWithdrawTime(
                    java.time.LocalDateTime.parse(request.getWithdrawTime().trim().replace(' ', 'T')));
        }
        if (request.hasDeltaBalance()) {
            domainReq.setDeltaBalance((long) request.getDeltaBalance());
            domainReq.setMinimumBalance(request.hasMinimumBalance() ? (long) request.getMinimumBalance() : 0L);
        }
        if (request.hasOperationKey()) {
            domainReq.setOperationKey(request.getOperationKey());
        }

        return withTransaction(() -> saldoCommandService.updateSaldoWithdraw(domainReq))
                .map(apiResp -> {
                    ApiResponseSaldo.Builder builder = ApiResponseSaldo.newBuilder()
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
    public Uni<ApiResponseSaldoDeleteAt> trashedSaldo(FindByIdSaldoRequest request) {
        markCurrentContextSafe();
        return withTransaction(() -> saldoCommandService.trash((long) request.getSaldoId()))
                .map(apiResp -> {
                    ApiResponseSaldoDeleteAt.Builder builder = ApiResponseSaldoDeleteAt.newBuilder()
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
    public Uni<ApiResponseSaldoDeleteAt> restoreSaldo(FindByIdSaldoRequest request) {
        markCurrentContextSafe();
        return withTransaction(() -> saldoCommandService.restore((long) request.getSaldoId()))
                .map(apiResp -> {
                    ApiResponseSaldoDeleteAt.Builder builder = ApiResponseSaldoDeleteAt.newBuilder()
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
    public Uni<ApiResponseSaldoDelete> deleteSaldoPermanent(FindByIdSaldoRequest request) {
        markCurrentContextSafe();
        return withTransaction(() -> saldoCommandService.delete((long) request.getSaldoId()))
                .map(apiResp -> ApiResponseSaldoDelete.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseSaldoAll> restoreAllSaldo(Empty request) {
        markCurrentContextSafe();
        return withTransaction(() -> saldoCommandService.restoreAll())
                .map(apiResp -> ApiResponseSaldoAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<ApiResponseSaldoAll> deleteAllSaldoPermanent(Empty request) {
        markCurrentContextSafe();
        return withTransaction(() -> saldoCommandService.deleteAll())
                .map(apiResp -> ApiResponseSaldoAll.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private void markCurrentContextSafe() {
        VertxContextSafetyToggle.setCurrentContextSafe(true);
    }

    private <T> Uni<T> withTransaction(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withTransaction(action);
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
}
