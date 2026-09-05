package com.sanedge.card.handler;

import com.sanedge.card.service.CardPaymentService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.card.CardPayment.CardPaymentResponse;
import pb.card.CardPayment.GetPaymentHistoryRequest;
import pb.card.CardPayment.PostPaymentRequest;
import pb.card.MutinyCardPaymentServiceGrpc;

@GrpcService
@Singleton
public class CardPaymentGrpcHandler
        extends MutinyCardPaymentServiceGrpc.CardPaymentServiceImplBase {

    @Inject
    CardPaymentService cardPaymentService;

    @Override
    public Uni<pb.card.CardPayment.ApiResponseCardPayment> postPayment(
            PostPaymentRequest request) {
        com.sanedge.card.domain.requests.PostPaymentRequest domainReq = new com.sanedge.card.domain.requests.PostPaymentRequest();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setAmount(java.math.BigDecimal.valueOf(request.getAmount()));
        domainReq.setPaymentChannel(request.getPaymentChannel());
        domainReq.setReferenceId(request.getReferenceId());

        return cardPaymentService.postPayment(domainReq)
                .map(apiResp -> {
                    pb.card.CardPayment.ApiResponseCardPayment.Builder builder = pb.card.CardPayment.ApiResponseCardPayment
                            .newBuilder()
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
    public Uni<pb.card.CardPayment.ApiResponsePaginationCardPayment> getPaymentHistory(
            GetPaymentHistoryRequest request) {
        return cardPaymentService.getPaymentHistory(
                request.getCardNumber(), request.getPage(), request.getPageSize())
                .map(apiResp -> {
                    pb.card.CardPayment.ApiResponsePaginationCardPayment.Builder builder = pb.card.CardPayment.ApiResponsePaginationCardPayment
                            .newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (com.sanedge.card.domain.response.CardPaymentResponse r : apiResp.data()) {
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
    public Uni<pb.card.CardPayment.ApiResponseCountPayment> countPayments(
            GetPaymentHistoryRequest request) {
        return cardPaymentService.countPayments(request.getCardNumber())
                .map(apiResp -> pb.card.CardPayment.ApiResponseCountPayment.newBuilder()
                        .setStatus(apiResp.status())
                        .setMessage(apiResp.message())
                        .setData(apiResp.data() != null ? apiResp.data() : 0L)
                        .build())
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private CardPaymentResponse toProto(
            com.sanedge.card.domain.response.CardPaymentResponse r) {
        if (r == null) {
            return CardPaymentResponse.getDefaultInstance();
        }
        CardPaymentResponse.Builder builder = CardPaymentResponse.newBuilder();
        if (r.getPaymentId() != null) {
            builder.setId(r.getPaymentId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount().doubleValue());
        }
        if (r.getPaymentChannel() != null) {
            builder.setPaymentChannel(r.getPaymentChannel());
        }
        if (r.getReferenceId() != null) {
            builder.setReferenceId(r.getReferenceId());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getPaidAt() != null) {
            builder.setPaidAt(r.getPaidAt());
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
}
