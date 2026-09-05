package com.sanedge.card.handler;

import com.sanedge.card.service.CardQueryService;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;

import pb.card.MutinyCardQueryServiceGrpc;
import pb.card.Card.FindAllCardRequest;
import pb.card.Card.FindByIdCardRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.ApiResponseCard;
import pb.card.CardQuery.ApiResponsePaginationCard;
import pb.card.CardQuery.ApiResponsePaginationCardDeleteAt;
import pb.user.UserQueryService;
import pb.user.User.FindByIdUserRequest;

import io.grpc.Status;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.grpc.GrpcService;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GrpcService
@Singleton
public class CardQueryGrpcHandler extends MutinyCardQueryServiceGrpc.CardQueryServiceImplBase {

    @Inject
    CardQueryService cardQueryService;

    @GrpcClient("user")
    UserQueryService userQueryService;

    @Override
    public Uni<ApiResponsePaginationCard> findAllCard(FindAllCardRequest request) {
        com.sanedge.card.domain.requests.FindAllCards domainReq = new com.sanedge.card.domain.requests.FindAllCards();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> cardQueryService.findAll(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationCard.Builder builder = ApiResponsePaginationCard.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CardResponse cr : apiResp.data()) {
                            builder.addData(toProto(cr));
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
    public Uni<ApiResponseCard> findByIdCard(FindByIdCardRequest request) {
        return withSession(() -> cardQueryService.findById((long) request.getCardId()))
                .map(apiResp -> {
                    ApiResponseCard.Builder builder = ApiResponseCard.newBuilder()
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
    public Uni<ApiResponseCard> findByUserIdCard(FindByUserIdCardRequest request) {
        return withSession(() -> cardQueryService.findByUserId((long) request.getUserId()))
                .map(apiResp -> {
                    ApiResponseCard.Builder builder = ApiResponseCard.newBuilder()
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
    public Uni<ApiResponsePaginationCardDeleteAt> findByActiveCard(FindAllCardRequest request) {
        com.sanedge.card.domain.requests.FindAllCards domainReq = new com.sanedge.card.domain.requests.FindAllCards();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> cardQueryService.findByActive(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationCardDeleteAt.Builder builder = ApiResponsePaginationCardDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CardResponseDeleteAt crd : apiResp.data()) {
                            builder.addData(toProto(crd));
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
    public Uni<ApiResponsePaginationCardDeleteAt> findByTrashedCard(FindAllCardRequest request) {
        com.sanedge.card.domain.requests.FindAllCards domainReq = new com.sanedge.card.domain.requests.FindAllCards();
        domainReq.setPage(request.getPage());
        domainReq.setPageSize(request.getPageSize());
        domainReq.setSearch(request.getSearch());

        return withSession(() -> cardQueryService.findByTrashed(domainReq))
                .map(apiResp -> {
                    ApiResponsePaginationCardDeleteAt.Builder builder = ApiResponsePaginationCardDeleteAt.newBuilder()
                            .setStatus(apiResp.status())
                            .setMessage(apiResp.message());
                    if (apiResp.data() != null) {
                        for (CardResponseDeleteAt crd : apiResp.data()) {
                            builder.addData(toProto(crd));
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
    public Uni<ApiResponseCard> findByCardNumber(FindByCardNumberRequest request) {
        return withSession(() -> cardQueryService.findByCardNumber(request.getCardNumber()))
                .map(apiResp -> {
                    ApiResponseCard.Builder builder = ApiResponseCard.newBuilder()
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
    public Uni<CardWithEmailResponse> findUserCardByCardNumber(FindByCardNumberRequest request) {
        return withSession(() -> cardQueryService.findByCardNumber(request.getCardNumber()))
                .chain(apiResp -> {
                    if (apiResp == null || apiResp.data() == null) {
                        return Uni.createFrom()
                                .failure(Status.NOT_FOUND.withDescription("Card not found").asRuntimeException());
                    }
                    CardResponse card = apiResp.data();
                    if (card.getUserId() == null) {
                        CardWithEmailResponse.Builder builder = CardWithEmailResponse.newBuilder();
                        populateCardWithEmail(builder, card, "");
                        return Uni.createFrom().item(builder.build());
                    }
                    return userQueryService
                            .findById(FindByIdUserRequest.newBuilder().setId(card.getUserId().intValue()).build())
                            .map(userResp -> {
                                String email = "";
                                if (userResp != null && userResp.hasData()) {
                                    email = userResp.getData().getEmail();
                                }
                                CardWithEmailResponse.Builder builder = CardWithEmailResponse.newBuilder();
                                populateCardWithEmail(builder, card, email);
                                return builder.build();
                            })
                            .onFailure().recoverWithItem(e -> {
                                CardWithEmailResponse.Builder builder = CardWithEmailResponse.newBuilder();
                                populateCardWithEmail(builder, card, "");
                                return builder.build();
                            });
                })
                .onFailure().transform(e -> {
                    if (e instanceof io.grpc.StatusRuntimeException) {
                        return e;
                    }
                    return Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException();
                });
    }

    private <T> Uni<T> withSession(java.util.function.Supplier<Uni<T>> action) {
        return Panache.withSession(action);
    }

    private void populateCardWithEmail(CardWithEmailResponse.Builder builder, CardResponse card, String email) {
        if (card.getId() != null) {
            builder.setId(card.getId().intValue());
        }
        if (card.getUserId() != null) {
            builder.setUserId(card.getUserId().intValue());
        }
        builder.setEmail(email != null ? email : "");
        if (card.getCardNumber() != null) {
            builder.setCardNumber(card.getCardNumber());
        }
        if (card.getCardType() != null) {
            builder.setCardType(card.getCardType());
        }
        if (card.getExpireDate() != null) {
            builder.setExpireDate(card.getExpireDate());
        }
        if (card.getCvv() != null) {
            builder.setCvv(card.getCvv());
        }
        if (card.getCardProvider() != null) {
            builder.setCardProvider(card.getCardProvider());
        }
        if (card.getCreatedAt() != null) {
            builder.setCreatedAt(card.getCreatedAt());
        }
        if (card.getUpdatedAt() != null) {
            builder.setUpdatedAt(card.getUpdatedAt());
        }
    }

    private pb.card.Card.CardResponse toProto(CardResponse r) {
        if (r == null) {
            return pb.card.Card.CardResponse.getDefaultInstance();
        }
        pb.card.Card.CardResponse.Builder builder = pb.card.Card.CardResponse.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getCardType() != null) {
            builder.setCardType(r.getCardType());
        }
        if (r.getExpireDate() != null) {
            builder.setExpireDate(r.getExpireDate());
        }
        if (r.getCvv() != null) {
            builder.setCvv(r.getCvv());
        }
        if (r.getCardProvider() != null) {
            builder.setCardProvider(r.getCardProvider());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
        }
        if (r.getUpdatedAt() != null) {
            builder.setUpdatedAt(r.getUpdatedAt());
        }
        return builder.build();
    }

    private pb.card.Card.CardResponseDeleteAt toProto(CardResponseDeleteAt r) {
        if (r == null) {
            return pb.card.Card.CardResponseDeleteAt.getDefaultInstance();
        }
        pb.card.Card.CardResponseDeleteAt.Builder builder = pb.card.Card.CardResponseDeleteAt.newBuilder();
        if (r.getId() != null) {
            builder.setId(r.getId().intValue());
        }
        if (r.getUserId() != null) {
            builder.setUserId(r.getUserId().intValue());
        }
        if (r.getCardNumber() != null) {
            builder.setCardNumber(r.getCardNumber());
        }
        if (r.getCardType() != null) {
            builder.setCardType(r.getCardType());
        }
        if (r.getExpireDate() != null) {
            builder.setExpireDate(r.getExpireDate());
        }
        if (r.getCvv() != null) {
            builder.setCvv(r.getCvv());
        }
        if (r.getCardProvider() != null) {
            builder.setCardProvider(r.getCardProvider());
        }
        if (r.getCreatedAt() != null) {
            builder.setCreatedAt(r.getCreatedAt());
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
