package com.sanedge.withdraw.service.impl;

import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.RequestFingerprint;
import com.sanedge.withdraw.domain.requests.CreateWithdrawRequest;
import com.sanedge.withdraw.domain.requests.UpdateWithdrawRequest;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.entity.Outbox;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.repository.OutboxRepository;
import com.sanedge.withdraw.repository.WithdrawCommandRepository;
import com.sanedge.withdraw.repository.WithdrawQueryRepository;
import com.sanedge.withdraw.service.KafkaService;
import com.sanedge.withdraw.service.WithdrawCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import pb.card.CardQueryService;
import pb.saldo.SaldoCommandService;
import pb.saldo.SaldoQueryService;

@ApplicationScoped
public class WithdrawCommandServiceImpl implements WithdrawCommandService {
    private static final Logger logger = LoggerFactory.getLogger(WithdrawCommandServiceImpl.class);

    private final WithdrawQueryRepository withdrawQueryRepository;
    private final WithdrawCommandRepository withdrawCommandRepository;
    private final CardQueryService cardQueryService;
    private final SaldoQueryService saldoQueryService;
    private final SaldoCommandService saldoCommandService;
    private final Validator validator;
    private final RedisService redisService;
    private final KafkaService kafkaService;
    private final OutboxRepository outboxRepository;
    private final TracingMetrics tracingMetrics;

    @Inject
    public WithdrawCommandServiceImpl(WithdrawQueryRepository withdrawQueryRepository,
            WithdrawCommandRepository withdrawCommandRepository,
            @GrpcClient("card") CardQueryService cardQueryService,
            @GrpcClient("saldo") SaldoQueryService saldoQueryService,
            @GrpcClient("saldo") SaldoCommandService saldoCommandService,
            Validator validator,
            RedisService redisService,
            KafkaService kafkaService,
            TracingMetrics tracingMetrics,
            OutboxRepository outboxRepository) {
        this.withdrawQueryRepository = withdrawQueryRepository;
        this.withdrawCommandRepository = withdrawCommandRepository;
        this.cardQueryService = cardQueryService;
        this.saldoQueryService = saldoQueryService;
        this.saldoCommandService = saldoCommandService;
        this.validator = validator;
        this.redisService = redisService;
        this.kafkaService = kafkaService;
        this.tracingMetrics = tracingMetrics;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Writes a withdraw.created stats event into the transactional outbox
     * (same DB transaction as the withdraw persist). Relayed to Kafka by
     * {@code OutboxPublisher}.
     */
    private Uni<Void> persistOutboxEvent(Withdraw withdraw) {
        Outbox outbox = new Outbox();
        outbox.setDomain("withdraw");
        outbox.setTopic("stats.payment.withdraw.event");
        outbox.setEventKey(String.valueOf(withdraw.getWithdrawId()));
        outbox.setEventId(java.util.UUID.randomUUID().toString());
        JsonObject payload = new JsonObject()
                .put("withdraw_id", withdraw.getWithdrawId())
                .put("card_number", withdraw.getCardNumber())
                .put("withdraw_amount", withdraw.getWithdrawAmount())
                .put("status", withdraw.getStatus() != null ? withdraw.getStatus().name() : null);
        outbox.setPayload(com.sanedge.common.event.EventEnvelope
                .withDefaults(payload, "withdraw.created")
                .encode());
        return outboxRepository.persist(outbox).replaceWithVoid();
    }

    private <T> void validateRequest(T req) {
        Set<ConstraintViolation<T>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (ConstraintViolation<T> violation : violations) {
                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage()).append("; ");
            }
            logger.error("Validation failed: {}", sb);
            throw new ConstraintViolationException("Validation failed: " + sb, violations);
        }
    }

    private Uni<Void> evictCaches(String cardNumber, Long withdrawId) {
        String key1 = "saldo:card:" + cardNumber;
        String key2 = "withdraws:id:" + withdrawId;
        String key3 = "withdraws:list:card:" + cardNumber;

        return Uni.combine().all().unis(
                redisService.deleteReactive(key1),
                redisService.deleteReactive(key2),
                redisService.deleteReactive(key3)).discardItems();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<WithdrawResponse>> create(CreateWithdrawRequest req) {
        String key = req.getIdempotencyKey();
        if (key == null || key.isBlank()) {
            return createInternal(req);
        }
        return withdrawQueryRepository.lockIdempotencyKey(key)
                .chain(v -> withdrawQueryRepository.findByIdempotencyKey(key))
                .chain(existing -> {
                    if (existing == null) {
                        return createInternal(req);
                    }
                    if (!sameRequest(existing, req)) {
                        return Uni.createFrom().failure(new ResourceAlreadyExistsException(
                                "Idempotency key is already used for a different withdraw request"));
                    }
                    return Uni.createFrom().item(ApiResponse.success("Withdraw request already processed",
                            WithdrawResponse.from(existing)));
                });
    }

    private boolean sameRequest(Withdraw existing, CreateWithdrawRequest req) {
        return existing.getRequestFingerprint() == null
                ? existing.getCardNumber().equals(req.getCardNumber())
                    && existing.getWithdrawAmount().longValue() == req.getWithdrawAmount()
                : existing.getRequestFingerprint().equals(RequestFingerprint.sha256(
                    req.getCardNumber(), String.valueOf(req.getWithdrawAmount())));
    }

    private Uni<ApiResponse<WithdrawResponse>> createInternal(CreateWithdrawRequest req) {
        try {
            validateRequest(req);
        } catch (Exception e) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed: " + e.getMessage(), null));
        }

        if (req.getWithdrawAmount() == null || req.getWithdrawAmount() <= 0) {
            return Uni.createFrom().item(new ApiResponse<>("error", "withdraw_amount must be > 0", null));
        }

        Attributes attrs = Attributes.builder().put("cardNumber", req.getCardNumber()).build();
        logger.info("Starting create withdraw request: {}", req);
        final Withdraw[] ledgerRef = new Withdraw[1];

        return tracingMetrics.traceAndMeasure("createWithdraw", "create", attrs, () -> {
            final String[] senderEmailContainer = new String[1];
            return cardQueryService
                    .findUserCardByCardNumber(
                            pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber(req.getCardNumber())
                                    .build())
                    .chain(cardResponse -> {
                        if (cardResponse == null || cardResponse.getCardNumber() == null
                                || cardResponse.getCardNumber().isEmpty()) {
                            logger.error("Card not found with number={}", req.getCardNumber());
                            throw new ResourceNotFoundException("Card not found");
                        }
                        senderEmailContainer[0] = cardResponse.getEmail();
                        return saldoQueryService.findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                                .setCardNumber(req.getCardNumber()).build());
                    })
                    .chain(saldoResponse -> {
                        if (saldoResponse == null || !saldoResponse.hasData()) {
                            logger.error("Saldo not found for card number={}", req.getCardNumber());
                            throw new ResourceNotFoundException("Saldo not found");
                        }
                        pb.saldo.Saldo.SaldoResponse saldo = saldoResponse.getData();

                        if (saldo.getTotalBalance() < req.getWithdrawAmount()) {
                            logger.error("Insufficient balance for card number={}. Balance={}, Requested={}",
                                    req.getCardNumber(), saldo.getTotalBalance(), req.getWithdrawAmount());
                            throw new IllegalStateException("Insufficient balance");
                        }

                        int newBalance = saldo.getTotalBalance() - req.getWithdrawAmount().intValue();

                        Withdraw withdraw = new Withdraw();
                        withdraw.setCardNumber(req.getCardNumber());
                        withdraw.setWithdrawNo(UUID.randomUUID());
                        withdraw.setStatus(Status.PENDING);
                        withdraw.setCompensationLegACard(req.getCardNumber());
                        withdraw.setCompensationLegADelta(-req.getWithdrawAmount().intValue());
                        ledgerRef[0] = withdraw;
                        withdraw.setWithdrawTime(
                                req.getWithdrawTime() != null ? java.sql.Timestamp.valueOf(req.getWithdrawTime())
                                        : java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        withdraw.setWithdrawAmount(req.getWithdrawAmount().intValue());
                        withdraw.setRequestFingerprint(RequestFingerprint.sha256(req.getCardNumber(),
                                String.valueOf(req.getWithdrawAmount())));
                        withdraw.setIdempotencyKey(req.getIdempotencyKey() == null || req.getIdempotencyKey().isBlank()
                                ? null : req.getIdempotencyKey());
                        withdraw.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        withdraw.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return saldoCommandService
                                .updateSaldoWithdraw(pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest.newBuilder()
                                        .setCardNumber(saldo.getCardNumber())
                                        .setTotalBalance(newBalance)
                                        .setDeltaBalance(-req.getWithdrawAmount().intValue())
                                        .setMinimumBalance(0)
                                        .setWithdrawTime(withdraw.getWithdrawTime().toString())
                                        .setWithdrawAmount(req.getWithdrawAmount().intValue())
                                        .setOperationKey("withdraw:" + withdraw.getWithdrawNo())
                                        .build())
                                .chain(v -> withdrawCommandRepository.persist(withdraw))
                                .chain(savedWithdraw -> withdrawCommandRepository
                                        .updateStatus(savedWithdraw.getWithdrawId(), Status.SUCCESS.toString()))
                                .chain(updatedWithdraw -> persistOutboxEvent(updatedWithdraw)
                                        .replaceWith(updatedWithdraw))
                                .chain(updatedWithdraw -> {
                                    logger.info("Withdraw created successfully with ID={}",
                                            updatedWithdraw.getWithdrawId());
                                    return evictCaches(req.getCardNumber(), updatedWithdraw.getWithdrawId())
                                            .chain(v -> {
                                                String email = senderEmailContainer[0];
                                                if (email != null && !email.isEmpty()) {
                                                    String emailSubject = "Withdraw Successful - SanEdge";
                                                    String emailBody = String.format(
                                                            "Hello,\n\nYour withdraw of %d has been processed successfully.\n\nRegards,\nSupport Team",
                                                            req.getWithdrawAmount().intValue());

                                                    JsonObject emailPayload = new JsonObject()
                                                            .put("email", email)
                                                            .put("subject", emailSubject)
                                                            .put("body", emailBody);

                                                    kafkaService
                                                            .sendMessage("email-service-topic-withdraw-create",
                                                                    String.valueOf(updatedWithdraw.getWithdrawId()),
                                                                    emailPayload)
                                                            .onFailure().invoke(e -> logger.warn("Kafka email failed for withdraw {}: {}", updatedWithdraw.getWithdrawId(), e.getMessage()))
                                                            .subscribe().with(x -> {}, x -> {});
                                                }

                                                return Uni.createFrom().item(ApiResponse.success("Created withdraw successfully",
                                                                WithdrawResponse.from(updatedWithdraw)));
                                            });
                                });
                    });
        }).onFailure().recoverWithUni(e -> {
            if (ledgerRef[0] != null && ledgerRef[0].getWithdrawId() != null) {
                return withdrawCommandRepository.markCompensationRequired(ledgerRef[0].getWithdrawId(), e.getMessage())
                        .map(ignored -> new ApiResponse<>("error",
                                "Withdraw requires reconciliation: " + e.getMessage(), null));
            }
            return Uni.createFrom().item(new ApiResponse<>("error", "Failed to create withdraw: " + e.getMessage(), null));
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<WithdrawResponse>> update(UpdateWithdrawRequest req) {
        try {
            validateRequest(req);
        } catch (Exception e) {
            return Uni.createFrom().item(new ApiResponse<>("error", "Validation failed: " + e.getMessage(), null));
        }

        if (req.getWithdrawId() == null) {
            return Uni.createFrom().item(new ApiResponse<>("error", "withdraw_id is required", null));
        }

        if (req.getWithdrawAmount() == null || req.getWithdrawAmount() <= 0) {
            return Uni.createFrom().item(new ApiResponse<>("error", "withdraw_amount must be > 0", null));
        }

        Attributes attrs = Attributes.builder().put("withdrawId", req.getWithdrawId()).build();
        logger.info("Starting update withdraw request: {}", req);

        return tracingMetrics.traceAndMeasure("updateWithdraw", "update", attrs, () -> {
            return cardQueryService
                    .findByCardNumber(
                            pb.card.Card.FindByCardNumberRequest.newBuilder().setCardNumber(req.getCardNumber())
                                    .build())
                    .chain(cardResponse -> {
                        if (cardResponse == null || !cardResponse.hasData()) {
                            logger.error("Card not found with number={}", req.getCardNumber());
                            throw new ResourceNotFoundException("Card not found");
                        }
                        return withdrawQueryRepository.findById(req.getWithdrawId());
                    })
                    .chain(withdraw -> {
                        if (withdraw == null) {
                            logger.error("Withdraw not found with ID={}", req.getWithdrawId());
                            throw new ResourceNotFoundException("Withdraw not found");
                        }
                        return saldoQueryService
                                .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                                        .setCardNumber(req.getCardNumber()).build())
                                .chain(saldoResponse -> {
                                    if (saldoResponse == null || !saldoResponse.hasData()) {
                                        logger.error("Saldo not found for card number={}", req.getCardNumber());
                                        throw new ResourceNotFoundException("Saldo not found");
                                    }
                                    pb.saldo.Saldo.SaldoResponse saldo = saldoResponse.getData();

                                    long amountDifference = req.getWithdrawAmount() - withdraw.getWithdrawAmount();
                                    if (saldo.getTotalBalance() < amountDifference) {
                                        logger.error("Insufficient balance for update. Balance={}, Needed={}",
                                                saldo.getTotalBalance(), amountDifference);
                                        throw new IllegalStateException("Insufficient balance");
                                    }

                                    int newBalance = saldo.getTotalBalance() - (int) amountDifference;

                                    withdraw.setCardNumber(req.getCardNumber());
                                    withdraw.setWithdrawAmount(req.getWithdrawAmount().intValue());
                                    withdraw.setWithdrawTime(req.getWithdrawTime() != null
                                            ? java.sql.Timestamp.valueOf(req.getWithdrawTime())
                                            : java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                                    withdraw.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                                    return saldoCommandService
                                            .updateSaldoWithdraw(
                                                    pb.saldo.SaldoCommand.UpdateSaldoWithdrawRequest.newBuilder()
                                                            .setCardNumber(saldo.getCardNumber())
                                                            .setTotalBalance(newBalance)
                                                            .setDeltaBalance(-((int) amountDifference))
                                                            .setMinimumBalance(0)
                                                            .setWithdrawTime(withdraw.getWithdrawTime().toString())
                                                            .setWithdrawAmount(req.getWithdrawAmount().intValue())
                                                            .build())
                                            .chain(v -> withdrawCommandRepository.persist(withdraw))
                                            .chain(savedWithdraw -> withdrawCommandRepository
                                                    .updateStatus(savedWithdraw.getWithdrawId(),
                                                            Status.SUCCESS.toString()))
                                            .chain(updatedWithdraw -> {
                                                logger.info("Withdraw updated successfully with ID={}",
                                                        updatedWithdraw.getWithdrawId());
                                                return evictCaches(req.getCardNumber(), updatedWithdraw.getWithdrawId())
                                                        .map(v -> ApiResponse.success("Updated withdraw successfully",
                                                                WithdrawResponse.from(updatedWithdraw)));
                                            });
                                });
                    });
        }).onFailure()
                .recoverWithItem(e -> new ApiResponse<>("error", "Failed to update withdraw: " + e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<WithdrawResponseDeleteAt>> trashed(Long withdrawId) {
        Attributes attrs = Attributes.builder().put("withdrawId", withdrawId).build();
        logger.info("Trashing withdraw id={}", withdrawId);

        return tracingMetrics.traceAndMeasure("trashWithdraw", "trash", attrs, () -> {
            return withdrawCommandRepository.trashed(withdrawId)
                    .chain(withdraw -> {
                        if (withdraw == null) {
                            throw new ResourceNotFoundException("Withdraw not found with ID " + withdrawId);
                        }
                        return evictCaches(withdraw.getCardNumber(), withdraw.getWithdrawId())
                                .map(v -> ApiResponse.success("Withdraw trashed successfully!",
                                        WithdrawResponseDeleteAt.from(withdraw)));
                    });
        }).onFailure()
                .recoverWithItem(e -> new ApiResponse<>("error", "Failed to trash withdraw: " + e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<WithdrawResponseDeleteAt>> restore(Long withdrawId) {
        Attributes attrs = Attributes.builder().put("withdrawId", withdrawId).build();
        logger.info("Restoring withdraw id={}", withdrawId);

        return tracingMetrics.traceAndMeasure("restoreWithdraw", "restore", attrs, () -> {
            return withdrawCommandRepository.restore(withdrawId)
                    .chain(withdraw -> {
                        if (withdraw == null) {
                            throw new InvalidRequestException("Withdraw not found or must be trashed first");
                        }
                        return evictCaches(withdraw.getCardNumber(), withdraw.getWithdrawId())
                                .map(v -> ApiResponse.success("Withdraw restored successfully!",
                                        WithdrawResponseDeleteAt.from(withdraw)));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deletePermanent(Long withdrawId) {
        Attributes attrs = Attributes.builder().put("withdrawId", withdrawId).build();
        logger.info("Permanently deleting withdraw id={}", withdrawId);

        return tracingMetrics.traceAndMeasure("deletePermanentWithdraw", "delete_permanent", attrs, () -> {
            return withdrawQueryRepository.findById(withdrawId)
                    .chain(withdraw -> {
                        if (withdraw == null || withdraw.getDeletedAt() == null) {
                            throw new InvalidRequestException(
                                    "Withdraw not found or must be trashed before permanent deletion");
                        }
                        return withdrawCommandRepository.deletePermanent(withdrawId)
                                .chain(success -> {
                                    if (!success) {
                                        throw new InvalidRequestException(
                                                "Withdraw not found or must be trashed before permanent deletion");
                                    }
                                    return evictCaches(withdraw.getCardNumber(), withdraw.getWithdrawId())
                                            .map(v -> ApiResponse.success("Withdraw permanently deleted!", true));
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed withdraws");

        return tracingMetrics.traceAndMeasure("restoreAllWithdraws", "restore_all", () -> {
            return withdrawCommandRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed withdraws found");
                        }
                        return ApiResponse.success("All withdraws restored successfully!", success);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed withdraws");

        return tracingMetrics.traceAndMeasure("deleteAllWithdraws", "delete_all", () -> {
            return withdrawCommandRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed withdraws found");
                        }
                        return ApiResponse.success("All withdraws permanently deleted!", success);
                    });
        });
    }
}