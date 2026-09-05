package com.sanedge.card.handler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.sanedge.card.service.BillingEngineService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.card.CardBilling.BillingStatementResponse;
import pb.card.CardBilling.GetStatementRequest;
import pb.card.CardBilling.GetStatementsByCardRequest;
import pb.card.CardBilling.TriggerBillingCycleRequest;
import pb.card.MutinyCardBillingServiceGrpc;

@GrpcService
@Singleton
public class CardBillingGrpcHandler
        extends MutinyCardBillingServiceGrpc.CardBillingServiceImplBase {

    @Inject
    BillingEngineService billingEngineService;

    @Override
    public Uni<pb.card.CardBilling.ApiResponseBillingStatement> triggerBillingCycle(
            TriggerBillingCycleRequest request) {
        return billingEngineService.triggerBillingCycle(request.getBillingCycleDay())
                .map(apiResp -> {
                    pb.card.CardBilling.ApiResponseBillingStatement.Builder builder =
                            pb.card.CardBilling.ApiResponseBillingStatement.newBuilder()
                                    .setStatus(apiResp.status())
                                    .setMessage(apiResp.message());
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    @Override
    public Uni<pb.card.CardBilling.ApiResponseBillingStatement> getStatement(
            GetStatementRequest request) {
        LocalDate statementDate = toLocalDate(request.getStatementDate());

        return billingEngineService.getStatement(request.getCardNumber(), statementDate)
                .map(apiResp -> {
                    pb.card.CardBilling.ApiResponseBillingStatement.Builder builder =
                            pb.card.CardBilling.ApiResponseBillingStatement.newBuilder()
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
    public Uni<pb.card.CardBilling.ApiResponsePaginationBillingStatement> getStatementsByCard(
            GetStatementsByCardRequest request) {
        return billingEngineService.getStatementsByCard(
                request.getCardNumber(), request.getPage(), request.getPageSize())
                .map(apiResp -> {
                    pb.card.CardBilling.ApiResponsePaginationBillingStatement.Builder builder =
                            pb.card.CardBilling.ApiResponsePaginationBillingStatement.newBuilder()
                                    .setStatus(apiResp.status())
                                    .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.card.domain.response.BillingStatementResponse r : apiResp.data()) {
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

    private BillingStatementResponse toProto(
            com.sanedge.card.domain.response.BillingStatementResponse r) {
        if (r == null) {
            return BillingStatementResponse.getDefaultInstance();
        }
        BillingStatementResponse.Builder builder = BillingStatementResponse.newBuilder();
        if (r.getStatementId() != null) {
            builder.setId(r.getStatementId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getBillingCycleDay() != null) {
            builder.setBillingCycleDay(r.getBillingCycleDay());
        }
        if (r.getOpeningBalance() != null) {
            builder.setOpeningBalance(r.getOpeningBalance().doubleValue());
        }
        if (r.getClosingBalance() != null) {
            builder.setClosingBalance(r.getClosingBalance().doubleValue());
        }
        if (r.getMinimumPayment() != null) {
            builder.setMinimumPayment(r.getMinimumPayment().doubleValue());
        }
        if (r.getDueDate() != null) {
            builder.setDueDate(r.getDueDate());
        }
        if (r.getFees() != null) {
            builder.setFees(r.getFees().doubleValue());
        }
        if (r.getInterest() != null) {
            builder.setInterest(r.getInterest().doubleValue());
        }
        if (r.getStatementDate() != null) {
            builder.setStatementDate(r.getStatementDate());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.common.PaginationMeta toProto(
            com.sanedge.common.domain.response.PaginationMeta m) {
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

    private LocalDate toLocalDate(com.google.protobuf.Timestamp timestamp) {
        if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
