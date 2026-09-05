package com.sanedge.card.handler;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.sanedge.card.service.CardAuthService;

import io.grpc.Status;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pb.card.CardAuthorization.AuthorizeCardRequest;
import pb.card.CardAuthorization.CardAuthTransactionResponse;
import pb.card.CardAuthorization.ReverseTransactionRequest;
import pb.card.MutinyCardAuthorizationServiceGrpc;

@GrpcService
@Singleton
public class CardAuthorizationGrpcHandler
        extends MutinyCardAuthorizationServiceGrpc.CardAuthorizationServiceImplBase {

    @Inject
    CardAuthService cardAuthService;

    @Override
    public Uni<pb.card.CardAuthorization.ApiResponseCardAuthTransaction> authorize(
            AuthorizeCardRequest request) {
        com.sanedge.card.domain.requests.AuthorizeCardRequest domainReq =
                new com.sanedge.card.domain.requests.AuthorizeCardRequest();
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setMerchantId(request.getMerchantId());
        domainReq.setAmount(java.math.BigDecimal.valueOf(request.getAmount()));
        domainReq.setCurrency(request.getCurrency());
        domainReq.setPosEntryMode(request.getPosEntryMode());
        domainReq.setMcc(request.getMcc());
        domainReq.setIdempotencyKey(request.getIdempotencyKey());

        return cardAuthService.authorize(domainReq)
                .map(apiResp -> {
                    pb.card.CardAuthorization.ApiResponseCardAuthTransaction.Builder builder =
                            pb.card.CardAuthorization.ApiResponseCardAuthTransaction.newBuilder()
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
    public Uni<pb.card.CardAuthorization.ApiResponseCardAuthTransaction> reverse(
            ReverseTransactionRequest request) {
        com.sanedge.card.domain.requests.ReverseTransactionRequest domainReq =
                new com.sanedge.card.domain.requests.ReverseTransactionRequest();
        domainReq.setAuthTxnId(request.getAuthTxnId());
        domainReq.setCardNumber(request.getCardNumber());
        domainReq.setAmount(java.math.BigDecimal.valueOf(request.getAmount()));
        domainReq.setIdempotencyKey(request.getIdempotencyKey());

        return cardAuthService.reverse(domainReq)
                .map(apiResp -> {
                    pb.card.CardAuthorization.ApiResponseCardAuthTransaction.Builder builder =
                            pb.card.CardAuthorization.ApiResponseCardAuthTransaction.newBuilder()
                                    .setStatus(apiResp.status())
                                    .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        builder.setData(toProto(apiResp.data()));
                    }
                    return builder.build();
                })
                .onFailure().transform(e -> Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
    }

    private CardAuthTransactionResponse toProto(
            com.sanedge.card.domain.response.CardAuthTransactionResponse r) {
        if (r == null) {
            return CardAuthTransactionResponse.getDefaultInstance();
        }
        CardAuthTransactionResponse.Builder builder = CardAuthTransactionResponse.newBuilder();
        if (r.getAuthTxnId() != null) {
            builder.setAuthTxnId(r.getAuthTxnId());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getMerchantId() != null) {
            builder.setMerchantId(r.getMerchantId());
        }
        if (r.getAmount() != null) {
            builder.setAmount(r.getAmount().doubleValue());
        }
        if (r.getCurrency() != null) {
            builder.setCurrency(r.getCurrency());
        }
        if (r.getPosEntryMode() != null) {
            builder.setPosEntryMode(r.getPosEntryMode());
        }
        if (r.getMcc() != null) {
            builder.setMcc(r.getMcc());
        }
        if (r.getIdempotencyKey() != null) {
            builder.setIdempotencyKey(r.getIdempotencyKey());
        }
        if (r.getRiskScore() != null) {
            builder.setRiskScore(r.getRiskScore());
        }
        if (r.getStatus() != null) {
            builder.setStatus(r.getStatus());
        }
        if (r.getAuthorizedAt() != null) {
            builder.setAuthorizedAt(r.getAuthorizedAt());
        }
        if (r.getReversedAt() != null) {
            builder.setReversedAt(r.getReversedAt());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }
}
