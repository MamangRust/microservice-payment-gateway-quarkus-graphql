package com.sanedge.common.adapter.card;

import java.time.Instant;

import com.google.protobuf.Timestamp;
import com.sanedge.common.adapter.model.Card;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.card.Card.CardResponse;
import pb.card.Card.CardWithEmailResponse;
import pb.card.Card.FindByCardNumberRequest;
import pb.card.Card.FindByUserIdCardRequest;
import pb.card.CardCommandService;
import pb.card.CardCommand.UpdateCardRequest;
import pb.card.CardQueryService;

@ApplicationScoped
public class CardAdapter implements CardPort {

    @GrpcClient("card")
    CardQueryService query;

    @GrpcClient("card")
    CardCommandService command;

    @Override
    public Uni<Card> findCardByUserId(int userId) {
        return query.findByUserIdCard(FindByUserIdCardRequest.newBuilder().setUserId(userId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Card not found for user: " + userId);
                    }
                    return toCard(resp.getData());
                });
    }

    @Override
    public Uni<Card> findUserCardByCardNumber(String cardNumber) {
        return query.findUserCardByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
                .map(resp -> {
                    if (resp == null || resp.getCardNumber() == null || resp.getCardNumber().isEmpty()) {
                        throw new ResourceNotFoundException("Card not found: " + cardNumber);
                    }
                    return toCard(resp);
                });
    }

    @Override
    public Uni<Card> findCardByCardNumber(String cardNumber) {
        return query.findByCardNumber(FindByCardNumberRequest.newBuilder().setCardNumber(cardNumber).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Card not found: " + cardNumber);
                    }
                    return toCard(resp.getData());
                });
    }

    @Override
    public Uni<Card> updateCard(UpdateData data) {
        UpdateCardRequest.Builder builder = UpdateCardRequest.newBuilder()
                .setCardId(data.cardId())
                .setUserId(data.userId())
                .setCardType(nullSafe(data.cardType()))
                .setCvv(nullSafe(data.cvv()))
                .setCardProvider(nullSafe(data.cardProvider()));
        Instant expire = ProtoTime.parse(data.expireDate());
        if (expire != null) {
            builder.setExpireDate(Timestamp.newBuilder().setSeconds(expire.getEpochSecond()).build());
        }
        return command.updateCard(builder.build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("Card not found: " + data.cardId());
                    }
                    return toCard(resp.getData());
                });
    }

    private static Card toCard(CardResponse r) {
        if (r == null) {
            return null;
        }
        return new Card(r.getId(), r.getUserId(), r.getCardNumber(), r.getCardType(), r.getExpireDate(),
                r.getCvv(), r.getCardProvider(), null,
                ProtoTime.parse(r.getCreatedAt()), ProtoTime.parse(r.getUpdatedAt()));
    }

    private static Card toCard(CardWithEmailResponse r) {
        if (r == null) {
            return null;
        }
        return new Card(r.getId(), r.getUserId(), r.getCardNumber(), r.getCardType(), r.getExpireDate(),
                r.getCvv(), r.getCardProvider(), r.getEmail(),
                ProtoTime.parse(r.getCreatedAt()), ProtoTime.parse(r.getUpdatedAt()));
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
