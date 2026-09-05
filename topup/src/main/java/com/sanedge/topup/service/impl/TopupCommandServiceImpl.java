package com.sanedge.topup.service.impl;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.RequestFingerprint;
import com.sanedge.topup.domain.requests.CreateTopupRequest;
import com.sanedge.topup.domain.requests.UpdateTopupRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.entity.Outbox;
import com.sanedge.topup.entity.Topup;
import com.sanedge.topup.repository.OutboxRepository;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import pb.card.CardQueryService;
import pb.saldo.SaldoQueryService;
import pb.saldo.SaldoCommandService;
import io.quarkus.grpc.GrpcClient;
import com.sanedge.topup.service.KafkaService;
import io.vertx.core.json.JsonObject;

import com.sanedge.topup.repository.TopupCommandRepository;
import com.sanedge.topup.repository.TopupQueryRepository;
import com.sanedge.topup.service.TopupCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@ApplicationScoped
public class TopupCommandServiceImpl implements TopupCommandService {
    private static final Logger logger = LoggerFactory.getLogger(TopupCommandServiceImpl.class);

    private final CardQueryService cardQueryService;
    private final SaldoQueryService saldoQueryService;
    private final SaldoCommandService saldoCommandService;
    private final TopupQueryRepository topupQueryRepository;
    private final TopupCommandRepository topupCommandRepository;
    private final Validator validator;
    private final RedisService redisService;
    private final KafkaService kafkaService;
    private final TracingMetrics tracingMetrics;
    private final OutboxRepository outboxRepository;

    @Inject
    public TopupCommandServiceImpl(@GrpcClient("card") CardQueryService cardQueryService,
            @GrpcClient("saldo") SaldoQueryService saldoQueryService,
            @GrpcClient("saldo") SaldoCommandService saldoCommandService,
            TopupQueryRepository topupQueryRepository,
            TopupCommandRepository topupCommandRepository,
            Validator validator,
            RedisService redisService,
            KafkaService kafkaService,
            TracingMetrics tracingMetrics,
            OutboxRepository outboxRepository) {
        this.cardQueryService = cardQueryService;
        this.saldoQueryService = saldoQueryService;
        this.saldoCommandService = saldoCommandService;
        this.topupQueryRepository = topupQueryRepository;
        this.topupCommandRepository = topupCommandRepository;
        this.validator = validator;
        this.redisService = redisService;
        this.kafkaService = kafkaService;
        this.tracingMetrics = tracingMetrics;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Writes a topup.created stats event into the transactional outbox (same DB
     * transaction as the topup persist). Relayed to Kafka by
     * {@code OutboxPublisher}.
     */
    private Uni<Void> persistOutboxEvent(Topup topup) {
        Outbox outbox = new Outbox();
        outbox.setDomain("topup");
        outbox.setTopic("stats.payment.topup.event");
        outbox.setEventKey(String.valueOf(topup.getTopupId()));
        outbox.setEventId(java.util.UUID.randomUUID().toString());
        JsonObject payload = new JsonObject()
                .put("topup_id", topup.getTopupId())
                .put("card_number", topup.getCardNumber())
                .put("topup_amount", topup.getTopupAmount())
                .put("topup_method", topup.getTopupMethod())
                .put("status", topup.getStatus() != null ? topup.getStatus().name() : null);
        outbox.setPayload(com.sanedge.common.event.EventEnvelope
                .withDefaults(payload, "topup.created")
                .encode());
        return outboxRepository.persist(outbox).replaceWithVoid();
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

    @Override
    @WithTransaction
    public Uni<ApiResponse<TopupResponse>> create(CreateTopupRequest req) {
        String key = req.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            return createInternal(req);
        }        return topupQueryRepository.lockIdempotencyKey(key)
                .chain(v -> topupQueryRepository.findByIdempotencyKey(key))
                .chain(existing -> {
                    if (existing == null) {
                        return createInternal(req);
                    }
                    if (!sameRequest(existing, req)) {
                        return Uni.createFrom().failure(new ResourceAlreadyExistsException(
                                "Idempotency key is already used for a different topup request"));
                    }
                    return Uni.createFrom().item(ApiResponse.success("Topup request already processed",
                            TopupResponse.from(existing)));
                });
    }

    private boolean sameRequest(Topup existing, CreateTopupRequest req) {
        return existing.getRequestFingerprint() == null
                ? existing.getCardNumber().equals(req.getCardNumber())
                    && existing.getTopupAmount().longValue() == req.getTopupAmount()
                    && existing.getTopupMethod().equals(req.getTopupMethod())
                : existing.getRequestFingerprint().equals(RequestFingerprint.sha256(
                    req.getCardNumber(), String.valueOf(req.getTopupAmount()), req.getTopupMethod()));
    }

    private Uni<ApiResponse<TopupResponse>> createInternal(CreateTopupRequest req) {
        if (!validateRequest(req)) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed", null));
        }

        Attributes attrs = Attributes.builder().put("cardNumber", req.getCardNumber()).build();
        logger.info("Starting CreateTopup: {}", req);
        final Topup[] ledgerRef = new Topup[1];

        return tracingMetrics.traceAndMeasure("createTopup", "create_topup", attrs, () -> {
            return cardQueryService
                    .findUserCardByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                            .setCardNumber(req.getCardNumber()).build())
                    .chain(cardWithEmail -> {
                        if (cardWithEmail == null || cardWithEmail.getCardNumber() == null
                                || cardWithEmail.getCardNumber().isEmpty()) {
                            logger.error("Card not found: {}", req.getCardNumber());
                            throw new ResourceNotFoundException("Card not found");
                        }

                        Topup topup = new Topup();
                        topup.setTopupNo(UUID.randomUUID());
                        topup.setCardNumber(req.getCardNumber());
                        topup.setTopupAmount(req.getTopupAmount().intValue());
                        topup.setTopupMethod(req.getTopupMethod());
                        topup.setIdempotencyKey(req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()
                                ? null : req.getIdempotencyKey());
                        topup.setRequestFingerprint(RequestFingerprint.sha256(req.getCardNumber(),
                                String.valueOf(req.getTopupAmount()), req.getTopupMethod()));
                        topup.setStatus(Status.PENDING);
                        topup.setCompensationLegACard(req.getCardNumber());
                        topup.setCompensationLegADelta(req.getTopupAmount().intValue());
                        ledgerRef[0] = topup;
                        topup.setTopupTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        topup.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        topup.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return topupCommandRepository.persist(topup)
                                .chain(savedTopup -> {
                                    return saldoQueryService
                                            .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                                                    .setCardNumber(req.getCardNumber()).build())
                                            .chain(saldoResponse -> {
                                                if (saldoResponse == null || !saldoResponse.hasData()) {
                                                    logger.error("Saldo not found for card: {}", req.getCardNumber());
                                                    return topupCommandRepository
                                                            .updateTopupStatus(savedTopup.getTopupId(), "FAILED")
                                                            .chain(v -> {
                                                                throw new ResourceNotFoundException("Saldo not found");
                                                            });
                                                }

                                                int newBalance = saldoResponse.getData().getTotalBalance()
                                                        + req.getTopupAmount().intValue();
                                                return saldoCommandService
                                                        .updateSaldoBalance(
                                                                pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest
                                                                        .newBuilder()
                                                                        .setCardNumber(
                                                                                saldoResponse.getData().getCardNumber())
                                                                        .setTotalBalance(newBalance)
                                                                        .setDeltaBalance(req.getTopupAmount().intValue())
                                                                        .setMinimumBalance(0)
                                                                        .setOperationKey("topup:" + savedTopup.getTopupId())
                                                                        .build())
                                                        .chain(v -> {
                                                            savedTopup.setCompensationLegAApplied(true);
                                                            savedTopup.setStatus(Status.SUCCESS);
                                                            savedTopup.setUpdatedAt(java.sql.Timestamp
                                                                    .valueOf(java.time.LocalDateTime.now()));
                                                            return topupCommandRepository.persist(savedTopup);
                                                        })
                                                        .chain(updatedTopup -> persistOutboxEvent(updatedTopup)
                                                                .replaceWith(updatedTopup))
                                                        .chain(updatedTopup -> {
                                                            logger.info(
                                                                    "CreateTopup completed: card={} topup_amount={} new_balance={}",
                                                                    req.getCardNumber(), req.getTopupAmount(),
                                                                    newBalance);

                                                            String topupCardCache = "topups:card:"
                                                                    + req.getCardNumber();
                                                            String topupIdCache = "topup:id:"
                                                                    + updatedTopup.getTopupId();
                                                            String saldoCardCache = "saldo:card:" + req.getCardNumber();
                                                            String saldoIdCache = "saldo:id:"
                                                                    + saldoResponse.getData().getSaldoId();

                                                            return Uni.combine().all().unis(
                                                                    redisService.deleteReactive(topupCardCache),
                                                                    redisService.deleteReactive(topupIdCache),
                                                                    redisService.deleteReactive(saldoCardCache),
                                                                    redisService.deleteReactive(saldoIdCache)).asTuple()
                                                                    .chain(t -> {
                                                                        if (cardWithEmail.getEmail() != null
                                                                                && !cardWithEmail.getEmail()
                                                                                        .isEmpty()) {
                                                                            String emailSubject = "Topup Successful - SanEdge";
                                                                            String emailBody = String.format(
                                                                                    "Hello,\n\nYour topup of %d has been processed successfully.\n\nRegards,\nSupport Team",
                                                                                    req.getTopupAmount().intValue());

                                                                            JsonObject emailPayload = new JsonObject()
                                                                                    .put("email",
                                                                                            cardWithEmail.getEmail())
                                                                                    .put("subject", emailSubject)
                                                                                    .put("body", emailBody);

                                                                            kafkaService.sendMessage(
                                                                                    "email-service-topic-topup-create",
                                                                                    String.valueOf(
                                                                                            updatedTopup.getTopupId()),
                                                                                    emailPayload)
                                                                                    .onFailure().invoke(e -> logger.warn("Kafka email failed for topup {}: {}", updatedTopup.getTopupId(), e.getMessage()))
                                                                                    .subscribe().with(v -> {}, e -> {});
                                                                        }

                                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                                "Topup created successfully for card="
                                                                                        + req.getCardNumber(),
                                                                                TopupResponse.from(updatedTopup)));
                                                                    });
                                                        });
                                            });
                                });
                    });
        }).onFailure().recoverWithUni(e -> {
            if (ledgerRef[0] != null && ledgerRef[0].getTopupId() != null) {
                return topupCommandRepository.markCompensationRequired(ledgerRef[0].getTopupId(), e.getMessage())
                        .map(ignored -> new ApiResponse<>("error",
                                "Topup requires reconciliation: " + e.getMessage(), null));
            }
            return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TopupResponse>> update(UpdateTopupRequest req) {
        if (!validateRequest(req)) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed", null));
        }

        Long topupId = req.getTopupId();
        if (topupId == null) {
            logger.error("topup_id is required");
            return Uni.createFrom().item(new ApiResponse<>("error", "topup_id is required", null));
        }

        Attributes attrs = Attributes.builder()
                .put("topupId", topupId)
                .put("cardNumber", req.getCardNumber())
                .build();
        logger.info("Starting UpdateTopup: {}", req);

        return tracingMetrics.traceAndMeasure("updateTopup", "update_topup", attrs, () -> {
            return cardQueryService
                    .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                            .setCardNumber(req.getCardNumber()).build())
                    .chain(cardResponse -> {
                        if (cardResponse == null || !cardResponse.hasData()) {
                            logger.error("Card not found: {}", req.getCardNumber());
                            return topupCommandRepository.updateTopupStatus(topupId, "FAILED")
                                    .chain(v -> {
                                        throw new ResourceNotFoundException("Card not found");
                                    });
                        }
                        return topupQueryRepository.findTopupById(topupId);
                    })
                    .chain(existingTopup -> {
                        if (existingTopup == null) {
                            logger.error("Topup {} not found", topupId);
                            return topupCommandRepository.updateTopupStatus(topupId, "FAILED")
                                    .chain(v -> {
                                        throw new ResourceNotFoundException("Topup not found");
                                    });
                        }

                        int difference = req.getTopupAmount().intValue() - existingTopup.getTopupAmount();

                        return saldoQueryService
                                .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                                        .setCardNumber(req.getCardNumber()).build())
                                .chain(saldoResponse -> {
                                    if (saldoResponse == null || !saldoResponse.hasData()) {
                                        logger.error("Saldo not found for card: {}", req.getCardNumber());
                                        return topupCommandRepository.updateTopupStatus(topupId, "FAILED")
                                                .chain(v -> {
                                                    throw new ResourceNotFoundException("Saldo not found");
                                                });
                                    }

                                    int newBalance = saldoResponse.getData().getTotalBalance() + difference;
                                    return saldoCommandService
                                            .updateSaldoBalance(
                                                    pb.saldo.SaldoCommand.UpdateSaldoBalanceRequest.newBuilder()
                                                            .setCardNumber(saldoResponse.getData().getCardNumber())
                                                            .setTotalBalance(newBalance)
                                                            .setDeltaBalance(difference)
                                                            .setMinimumBalance(0)
                                                            .build())
                                            .chain(v -> {
                                                existingTopup.setTopupAmount(req.getTopupAmount().intValue());
                                                existingTopup.setStatus(Status.SUCCESS);
                                                existingTopup.setTopupMethod(req.getTopupMethod());
                                                existingTopup.setUpdatedAt(
                                                        java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                                                return topupCommandRepository.persist(existingTopup);
                                            })
                                            .chain(updatedTopup -> {
                                                logger.info(
                                                        "UpdateTopup completed: card={} topup_id={} new_amount={} new_balance={}",
                                                        req.getCardNumber(), topupId, req.getTopupAmount(), newBalance);

                                                String topupCardCache = "topups:card:" + req.getCardNumber();
                                                String topupIdCache = "topup:id:" + updatedTopup.getTopupId();
                                                String saldoCardCache = "saldo:card:" + req.getCardNumber();
                                                String saldoIdCache = "saldo:id:"
                                                        + saldoResponse.getData().getSaldoId();

                                                return Uni.combine().all().unis(
                                                        redisService.deleteReactive(topupCardCache),
                                                        redisService.deleteReactive(topupIdCache),
                                                        redisService.deleteReactive(saldoCardCache),
                                                        redisService.deleteReactive(saldoIdCache)).asTuple()
                                                        .map(t -> ApiResponse.success(
                                                                "Topup updated successfully for card="
                                                                        + req.getCardNumber(),
                                                                TopupResponse.from(updatedTopup)));
                                            });
                                });
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TopupResponseDeleteAt>> trashed(Long topupId) {
        Attributes attrs = Attributes.builder().put("topupId", topupId).build();
        logger.info("Trashing topup id={}", topupId);

        return tracingMetrics.traceAndMeasure("trashTopup", "trash_topup", attrs, () -> {
            return topupCommandRepository.trashed(topupId)
                    .chain(topup -> {
                        if (topup == null) {
                            logger.error("Topup not found with id {}", topupId);
                            throw new ResourceNotFoundException("Topup not found");
                        }

                        String topupCardCache = "topups:card:" + topup.getCardNumber();
                        String topupIdCache = "topup:id:" + topupId;

                        return Uni.combine().all().unis(
                                redisService.deleteReactive(topupCardCache),
                                redisService.deleteReactive(topupIdCache)).asTuple()
                                .map(t -> ApiResponse.success("Trashed topup id=" + topupId,
                                        TopupResponseDeleteAt.from(topup)));
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<TopupResponseDeleteAt>> restore(Long topupId) {
        Attributes attrs = Attributes.builder().put("topupId", topupId).build();
        logger.info("Restoring topup id={}", topupId);

        return tracingMetrics.traceAndMeasure("restoreTopup", "restore_topup", attrs, () -> {
            return topupCommandRepository.restore(topupId)
                    .chain(topup -> {
                        if (topup == null) {
                            logger.error("Topup restore failed - topup not found or must be trashed first with id {}", topupId);
                            throw new InvalidRequestException("Topup not found or must be trashed first");
                        }

                        String topupCardCache = "topups:card:" + topup.getCardNumber();
                        String topupIdCache = "topup:id:" + topupId;

                        return Uni.combine().all().unis(
                                redisService.deleteReactive(topupCardCache),
                                redisService.deleteReactive(topupIdCache)).asTuple()
                                .map(t -> ApiResponse.success("Restored topup id=" + topupId,
                                        TopupResponseDeleteAt.from(topup)));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deletePermanent(Long topupId) {
        Attributes attrs = Attributes.builder().put("topupId", topupId).build();
        logger.info("Permanently deleting topup id={}", topupId);

        return tracingMetrics.traceAndMeasure("deleteTopupPermanent", "delete_topup_permanent", attrs, () -> {
            return topupQueryRepository.findTopupById(topupId)
                    .chain(topup -> {
                        if (topup == null || topup.getDeletedAt() == null) {
                            logger.error("Permanent delete failed - topup not found or must be trashed before permanent deletion with id {}", topupId);
                            throw new InvalidRequestException("Topup not found or must be trashed before permanent deletion");
                        }

                        String topupCardCache = "topups:card:" + topup.getCardNumber();
                        String topupIdCache = "topup:id:" + topupId;

                        return topupCommandRepository.deletePermanent(topupId)
                                .chain(deleted -> Uni.combine().all().unis(
                                        redisService.deleteReactive(topupCardCache),
                                        redisService.deleteReactive(topupIdCache)).asTuple()
                                        .map(t -> ApiResponse.success("Deleted topup id=" + topupId, true)));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed topups");

        return tracingMetrics.traceAndMeasure("restoreAllTopups", "restore_all_topups", () -> {
            return topupCommandRepository.restoreAllDeleted()
                    .map(restored -> {
                        if (!restored) {
                            throw new ResourceNotFoundException("No trashed topups found");
                        }
                        return ApiResponse.success("Restored all trashed topups", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed topups");

        return tracingMetrics.traceAndMeasure("deleteAllTopups", "delete_all_topups", () -> {
            return topupCommandRepository.deleteAllDeleted()
                    .map(deleted -> {
                        if (!deleted) {
                            throw new ResourceNotFoundException("No trashed topups found");
                        }
                        return ApiResponse.success("Deleted all trashed topups", true);
                    });
        });
    }
}