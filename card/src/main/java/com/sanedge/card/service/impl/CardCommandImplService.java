package com.sanedge.card.service.impl;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.domain.requests.CreateCardRequest;
import com.sanedge.card.domain.requests.UpdateCardRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.entity.Card;
import com.sanedge.card.entity.Outbox;
import pb.user.UserQueryService;
import pb.user.User.FindByIdUserRequest;
import io.quarkus.grpc.GrpcClient;
import com.sanedge.card.service.KafkaService;
import io.vertx.core.json.JsonObject;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.repository.OutboxRepository;
import com.sanedge.card.service.CardCommandService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.CardNumberGenerator;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;

@ApplicationScoped
public class CardCommandImplService implements CardCommandService {
    private static final Logger logger = LoggerFactory.getLogger(CardCommandImplService.class);

    @Inject
    CardCommandRepository cardCommandRepository;

    @Inject
    CardQueryRepository cardQueryRepository;

    @GrpcClient("user")
    UserQueryService userQueryService;

    @Inject
    KafkaService kafkaService;

    @Inject
    OutboxRepository outboxRepository;

    @Inject
    Validator validator;

    @Inject
    TracingMetrics tracingMetrics;

    /**
     * Writes a card.created stats event into the transactional outbox (same DB
     * transaction as the card persist). Relayed to Kafka by {@code OutboxPublisher}.
     */
    private Uni<Void> persistOutboxEvent(Card card) {
        Outbox outbox = new Outbox();
        outbox.setDomain("card");
        outbox.setTopic("stats.payment.card.event");
        outbox.setEventKey(String.valueOf(card.getCardId()));
        outbox.setEventId(java.util.UUID.randomUUID().toString());
        JsonObject payload = new JsonObject()
                .put("card_id", card.getCardId())
                .put("user_id", card.getUserId())
                .put("card_number", card.getCardNumber())
                .put("card_type", card.getCardType())
                .put("status", card.getStatus() != null ? card.getStatus().name() : null);
        outbox.setPayload(com.sanedge.common.event.EventEnvelope
                .withDefaults(payload, "card.created")
                .encode());
        return outboxRepository.persist(outbox).replaceWithVoid();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardResponse>> createCard(CreateCardRequest req) {
        if (!validateRequest(req)) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed", null));
        }

        if (req.getUserId() == null) {
            logger.error("user_id is required");
            return Uni.createFrom().item(new ApiResponse<>("error", "user_id is required", null));
        }

        Attributes attrs = Attributes.builder().put("user.id", req.getUserId()).build();
        logger.info("Creating card for user_id={}", req.getUserId());

        return tracingMetrics.traceAndMeasure("createCard", "create_card", attrs, () -> {
            return userQueryService.findById(FindByIdUserRequest.newBuilder().setId(req.getUserId().intValue()).build())
                    .chain(response -> {
                        if (response == null || !response.hasData()) {
                            logger.error("User with id {} not found", req.getUserId());
                            throw new IllegalArgumentException("User not found");
                        }

                        Card card = new Card();
                        try {
                            String cardNumber = CardNumberGenerator.randomCardNumber();
                            card.setCardNumber(cardNumber);
                        } catch (Exception e) {
                            logger.error("Failed to generate card number", e);
                            throw new RuntimeException("Failed to generate card number");
                        }

                        card.setUserId(req.getUserId().intValue());
                        card.setCardType(req.getCardType());
                        card.setExpireDate(java.sql.Date.valueOf(req.getExpireDate()));
                        card.setCvv(req.getCvv());
                        card.setCardProvider(req.getCardProvider());

                        return cardCommandRepository.persist(card)
                                .chain(savedCard -> persistOutboxEvent(savedCard)
                                        .invoke(() -> {
                                            JsonObject saldoPayload = new JsonObject()
                                                    .put("card_number", savedCard.getCardNumber())
                                                    .put("total_balance", 0);
                                            kafkaService.sendMessage(
                                                    "saldo-service-topic-create-saldo",
                                                    savedCard.getCardNumber(),
                                                    saldoPayload)
                                                    .onFailure().recoverWithNull();
                                        })
                                        .map(v -> savedCard))
                                .map(savedCard -> {
                                    CardResponse cardResp = CardResponse.from(savedCard);
                                    logger.info("Card created successfully with card_id={}", cardResp.getId());
                                    return new ApiResponse<>("success", "Card created successfully", cardResp);
                                });
                    });
        }).onFailure().recoverWithItem(e -> {
            logger.error("Failed to create card for user_id={}", req.getUserId(), e);
            String msg = "Failed to create card";
            if (e instanceof IllegalArgumentException) {
                msg = e.getMessage();
            }
            return new ApiResponse<>("error", msg, null);
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardResponse>> updateCard(UpdateCardRequest req) {
        if (!validateRequest(req)) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed", null));
        }

        if (req.getCardId() == null) {
            logger.error("card_id is required");
            return Uni.createFrom().item(new ApiResponse<>("error", "card_id is required", null));
        }

        if (req.getUserId() == null) {
            logger.error("user_id is required");
            return Uni.createFrom().item(new ApiResponse<>("error", "user_id is required", null));
        }

        Attributes attrs = Attributes.builder()
                .put("card.id", req.getCardId())
                .put("user.id", req.getUserId())
                .build();
        logger.info("Updating card id={} for user_id={}", req.getCardId(), req.getUserId());

        return tracingMetrics.traceAndMeasure("updateCard", "update_card", attrs, () -> {
            return userQueryService.findById(FindByIdUserRequest.newBuilder().setId(req.getUserId().intValue()).build())
                    .chain(response -> {
                        if (response == null || !response.hasData()) {
                            logger.error("User with id {} not found", req.getUserId());
                            throw new IllegalArgumentException("User not found");
                        }
                        return cardQueryRepository.findById(req.getCardId());
                    })
                    .chain(card -> {
                        if (card == null) {
                            logger.error("Card with id {} not found", req.getCardId());
                            throw new IllegalArgumentException("Card not found");
                        }

                        card.setCardType(req.getCardType());
                        card.setExpireDate(java.sql.Date.valueOf(req.getExpireDate()));
                        card.setCvv(req.getCvv());
                        card.setCardProvider(req.getCardProvider());

                        return cardCommandRepository.persist(card)
                                .map(updatedCard -> {
                                    CardResponse response = CardResponse.from(updatedCard);
                                    logger.info("Card updated successfully with card_id={}", response.getId());
                                    return new ApiResponse<>("success", "Card updated successfully", response);
                                });
                    });
        }).onFailure().recoverWithItem(e -> {
            logger.error("Failed to update card id={} for user_id={}", req.getCardId(), req.getUserId(), e);
            String msg = "Failed to update card";
            if (e instanceof IllegalArgumentException) {
                msg = e.getMessage();
            }
            return new ApiResponse<>("error", msg, null);
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardResponseDeleteAt>> trashCard(Long id) {
        Attributes attrs = Attributes.builder().put("card.id", id).build();
        logger.info("Trashing card id={}", id);

        return tracingMetrics.traceAndMeasure("trashCard", "trash_card", attrs, () -> {
            return cardCommandRepository.trashed(id)
                    .map(card -> {
                        if (card == null) {
                            throw new IllegalArgumentException("Card not found");
                        }
                        return new ApiResponse<>("success", "Card trashed successfully",
                                CardResponseDeleteAt.from(card));
                    });
        }).onFailure().recoverWithItem(e -> {
            logger.error("Failed to trash card id={}", id, e);
            return new ApiResponse<>("error", "Failed to trash card", null);
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardResponseDeleteAt>> restoreCard(Long id) {
        Attributes attrs = Attributes.builder().put("card.id", id).build();
        logger.info("Restoring card id={}", id);

        return tracingMetrics.traceAndMeasure("restoreCard", "restore_card", attrs, () -> {
            return cardCommandRepository.restore(id)
                    .map(card -> {
                        if (card == null) {
                            throw new InvalidRequestException("Card not found or must be trashed first");
                        }
                        return new ApiResponse<>("success", "Card restored successfully",
                                CardResponseDeleteAt.from(card));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteCard(Long id) {
        Attributes attrs = Attributes.builder().put("card.id", id).build();
        logger.info("Permanently deleting card id={}", id);

        return tracingMetrics.traceAndMeasure("deleteCard", "delete_card", attrs, () -> {
            return cardCommandRepository.deletePermanent(id)
                    .map(deleted -> {
                        if (!deleted.isPersistent()) {
                            throw new InvalidRequestException(
                                    "Card not found or must be trashed before permanent deletion");
                        }
                        return new ApiResponse<>("success", "Card permanently deleted", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed cards");
        return tracingMetrics.traceAndMeasure("restoreAllCards", "restore_all_cards", () -> {
            return cardCommandRepository.restoreAllDeleted()
                    .map(restored -> {
                        if (!restored) {
                            throw new ResourceNotFoundException("No trashed cards found");
                        }
                        return new ApiResponse<>("success", "All cards restored successfully", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed cards");
        return tracingMetrics.traceAndMeasure("deleteAllCards", "delete_all_cards", () -> {
            return cardCommandRepository.deleteAllDeleted()
                    .map(deleted -> {
                        if (!deleted) {
                            throw new ResourceNotFoundException("No trashed cards found");
                        }
                        return new ApiResponse<>("success", "All cards permanently deleted", true);
                    });
        });
    }

    private <T> boolean validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath())
                        .append(": ")
                        .append(violation.getMessage())
                        .append("; ");
            }
            logger.error("Validation failed: {}", sb);
            return false;
        }
        return true;
    }
}