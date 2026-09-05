package com.sanedge.saldo.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.saldo.domain.requests.CreateSaldoRequest;
import com.sanedge.saldo.domain.requests.UpdateSaldoRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.saldo.entity.Outbox;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.repository.OutboxRepository;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import pb.card.CardQueryService;
import io.quarkus.grpc.GrpcClient;
import com.sanedge.saldo.repository.SaldoCommandRepository;
import com.sanedge.saldo.repository.SaldoQueryRepository;
import com.sanedge.saldo.service.SaldoCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SaldoCommandServiceImpl implements SaldoCommandService {
    private static final Logger logger = LoggerFactory.getLogger(SaldoCommandServiceImpl.class);

    private final CardQueryService cardQueryService;
    private final SaldoCommandRepository saldoCommandRepository;
    private final SaldoQueryRepository saldoQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final OutboxRepository outboxRepository;

    @Inject
    public SaldoCommandServiceImpl(@GrpcClient("card") CardQueryService cardQueryService,
            SaldoCommandRepository saldoCommandRepository,
            SaldoQueryRepository saldoQueryRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics,
            OutboxRepository outboxRepository) {
        this.cardQueryService = cardQueryService;
        this.saldoCommandRepository = saldoCommandRepository;
        this.saldoQueryRepository = saldoQueryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
        this.outboxRepository = outboxRepository;
    }

    /**
     * Writes a saldo.balance.changed stats event into the transactional outbox
     * (same DB transaction as the saldo persist). Relayed to Kafka by
     * {@code OutboxPublisher}.
     */
    private Uni<Void> persistOutboxEvent(Saldo saldo) {
        Outbox outbox = new Outbox();
        outbox.setDomain("saldo");
        outbox.setTopic("stats.payment.saldo.event");
        outbox.setEventKey(saldo.getCardNumber());
        outbox.setEventId(java.util.UUID.randomUUID().toString());
        JsonObject payload = new JsonObject()
                .put("card_number", saldo.getCardNumber())
                .put("total_balance", saldo.getTotalBalance());
        outbox.setPayload(com.sanedge.common.event.EventEnvelope
                .withDefaults(payload, "saldo.balance.changed")
                .encode());
        return outboxRepository.persist(outbox).replaceWithVoid();
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponse>> create(CreateSaldoRequest request) {
        Attributes attrs = Attributes.builder().put("cardNumber", request.getCardNumber()).build();
        logger.info("Creating saldo for card_number={}", request.getCardNumber());

        return tracingMetrics.traceAndMeasure("createSaldo", "create_saldo", attrs, () -> {
            return cardQueryService
                    .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                            .setCardNumber(request.getCardNumber()).build())
                    .chain(cardResponse -> {
                        if (cardResponse == null || !cardResponse.hasData()) {
                            logger.error("Card {} not found", request.getCardNumber());
                            throw new ResourceNotFoundException("Card not found");
                        }

                        // Kafka saldo-create is at-least-once. Re-delivery must replay the
                        // existing saldo instead of inserting a second row for the card.
                        return saldoCommandRepository.lockCardForCreate(request.getCardNumber())
                                .chain(() -> saldoQueryRepository.findByCardNumber(request.getCardNumber()))
                                .chain(existing -> {
                                    if (existing != null) {
                                        return Uni.createFrom().item(ApiResponse.success("Saldo already exists",
                                                SaldoResponse.from(existing)));
                                    }
                                    return persistNewSaldo(request);
                                });
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    private Uni<ApiResponse<SaldoResponse>> persistNewSaldo(CreateSaldoRequest request) {
                        Saldo saldo = new Saldo();
                        saldo.setCardNumber(request.getCardNumber());
                        saldo.setTotalBalance(request.getTotalBalance().intValue());
                        saldo.setWithdrawAmount(0);
                        saldo.setWithdrawTime(null);
                        saldo.setCreatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        saldo.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return saldoCommandRepository.persist(saldo)
                                .chain(savedSaldo -> persistOutboxEvent(savedSaldo)
                                        .replaceWith(savedSaldo))
                                .chain(savedSaldo -> {
                                    logger.info("Saldo created successfully with id={} for card={}",
                                            savedSaldo.getSaldoId(), request.getCardNumber());
                                    String cacheKey = "saldo:card:" + request.getCardNumber();

                                    return redisService.deleteReactive(cacheKey)
                                            .map(v -> ApiResponse.success("Create saldo success",
                                                    SaldoResponse.from(savedSaldo)));
                                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponse>> update(UpdateSaldoRequest request) {
        if (request.getSaldoId() == null) {
            logger.error("saldo_id is required");
            return Uni.createFrom().item(new ApiResponse<>("error", "saldo_id is required", null));
        }

        Attributes attrs = Attributes.builder()
                .put("saldo.id", request.getSaldoId())
                .put("cardNumber", request.getCardNumber())
                .build();
        logger.info("Updating saldo id={} for card={}", request.getSaldoId(), request.getCardNumber());

        return tracingMetrics.traceAndMeasure("updateSaldo", "update_saldo", attrs, () -> {
            return cardQueryService
                    .findByCardNumber(pb.card.Card.FindByCardNumberRequest.newBuilder()
                            .setCardNumber(request.getCardNumber()).build())
                    .chain(cardResponse -> {
                        if (cardResponse == null || !cardResponse.hasData()) {
                            logger.error("Card {} not found during update", request.getCardNumber());
                            throw new ResourceNotFoundException("Card not found");
                        }
                        return saldoQueryRepository.findById(request.getSaldoId());
                    })
                    .chain(saldo -> {
                        if (saldo == null) {
                            logger.error("Saldo not found with id {}", request.getSaldoId());
                            throw new ResourceNotFoundException("Saldo not found");
                        }

                        saldo.setCardNumber(request.getCardNumber());
                        saldo.setTotalBalance(request.getTotalBalance().intValue());
                        saldo.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return saldoCommandRepository.persist(saldo)
                                .chain(updatedSaldo -> {
                                    logger.info("Saldo updated successfully with id={} for card={}",
                                            updatedSaldo.getSaldoId(), request.getCardNumber());
                                    String cacheId = "saldo:id:" + request.getSaldoId();
                                    String cacheCard = "saldo:card:" + request.getCardNumber();

                                    return Uni.combine().all().unis(
                                            redisService.deleteReactive(cacheId),
                                            redisService.deleteReactive(cacheCard)).asTuple()
                                            .map(t -> ApiResponse.success("Update saldo success",
                                                    SaldoResponse.from(updatedSaldo)));
                                });
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponseDeleteAt>> trash(Long id) {
        Attributes attrs = Attributes.builder().put("saldo.id", id).build();
        logger.info("Trashing saldo id={}", id);

        return tracingMetrics.traceAndMeasure("trashSaldo", "trash_saldo", attrs, () -> {
            return saldoCommandRepository.trashed(id)
                    .chain(saldo -> {
                        if (saldo == null) {
                            logger.error("Saldo not found with id {}", id);
                            throw new ResourceNotFoundException("Saldo not found");
                        }

                        String cacheId = "saldo:id:" + id;
                        String cacheCard = "saldo:card:" + saldo.getCardNumber();

                        return Uni.combine().all().unis(
                                redisService.deleteReactive(cacheId),
                                redisService.deleteReactive(cacheCard)).asTuple()
                                .map(t -> ApiResponse.success("Trash saldo success",
                                        SaldoResponseDeleteAt.from(saldo)));
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponseDeleteAt>> restore(Long id) {
        Attributes attrs = Attributes.builder().put("saldo.id", id).build();
        logger.info("Restoring saldo id={}", id);

        return tracingMetrics.traceAndMeasure("restoreSaldo", "restore_saldo", attrs, () -> {
            return saldoCommandRepository.restore(id)
                    .chain(saldo -> {
                        if (saldo == null) {
                            logger.error("Saldo restore failed - saldo not found or must be trashed first with id {}", id);
                            throw new InvalidRequestException("Saldo not found or must be trashed first");
                        }

                        String cacheId = "saldo:id:" + id;
                        String cacheCard = "saldo:card:" + saldo.getCardNumber();

                        return Uni.combine().all().unis(
                                redisService.deleteReactive(cacheId),
                                redisService.deleteReactive(cacheCard)).asTuple()
                                .map(t -> ApiResponse.success("Restore saldo success",
                                        SaldoResponseDeleteAt.from(saldo)));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> delete(Long id) {
        Attributes attrs = Attributes.builder().put("saldo.id", id).build();
        logger.info("Permanently deleting saldo id={}", id);

        return tracingMetrics.traceAndMeasure("deleteSaldo", "delete_saldo", attrs, () -> {
            return saldoQueryRepository.findById(id)
                    .chain(saldo -> {
                        if (saldo == null || saldo.getDeletedAt() == null) {
                            logger.error("Permanent delete failed - saldo not found or must be trashed before permanent deletion with id {}", id);
                            throw new InvalidRequestException("Saldo not found or must be trashed before permanent deletion");
                        }

                        String cacheId = "saldo:id:" + id;
                        String cacheCard = "saldo:card:" + saldo.getCardNumber();

                        return saldoCommandRepository.deletePermanent(id)
                                .chain(deleted -> Uni.combine().all().unis(
                                        redisService.deleteReactive(cacheId),
                                        redisService.deleteReactive(cacheCard)).asTuple()
                                        .map(t -> ApiResponse.success("Delete saldo success", true)));
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> restoreAll() {
        logger.info("Restoring ALL trashed saldos");

        return tracingMetrics.traceAndMeasure("restoreAllSaldos", "restore_all_saldos", () -> {
            return saldoCommandRepository.restoreAllDeleted()
                    .map(restored -> {
                        if (!restored) {
                            throw new ResourceNotFoundException("No trashed saldos found");
                        }
                        return ApiResponse.success("Restore all saldo success", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Boolean>> deleteAll() {
        logger.info("Permanently deleting ALL trashed saldos");

        return tracingMetrics.traceAndMeasure("deleteAllSaldos", "delete_all_saldos", () -> {
            return saldoCommandRepository.deleteAllDeleted()
                    .map(deleted -> {
                        if (!deleted) {
                            throw new ResourceNotFoundException("No trashed saldos found");
                        }
                        return ApiResponse.success("Delete all saldo success", true);
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponse>> updateSaldoBalance(
            com.sanedge.saldo.domain.requests.UpdateSaldoBalance request) {
        Attributes attrs = Attributes.builder().put("cardNumber", request.getCardNumber()).build();
        logger.info("Updating saldo balance for card_number={}", request.getCardNumber());

        return tracingMetrics.traceAndMeasure("updateSaldoBalance", "update_saldo_balance", attrs, () -> {
            if (request.getDeltaBalance() != null) {
                return saldoCommandRepository.updateBalanceByDelta(request.getCardNumber(), request.getDeltaBalance(),
                                request.getMinimumBalance(), request.getOperationKey())
                        .chain(updated -> {
                            if (updated == 0) {
                                return saldoQueryRepository.findByCardNumber(request.getCardNumber())
                                        .chain(existing -> existing == null
                                                ? Uni.createFrom().failure(new ResourceNotFoundException("Saldo not found"))
                                                : Uni.createFrom().failure(new IllegalStateException(
                                                        "Insufficient balance for requested saldo mutation")));
                            }
                            return saldoQueryRepository.findByCardNumber(request.getCardNumber());
                        })
                        .chain(updatedSaldo -> {
                            String cacheId = "saldo:id:" + updatedSaldo.getSaldoId();
                            String cacheCard = "saldo:card:" + request.getCardNumber();
                            return Uni.combine().all().unis(
                                    redisService.deleteReactive(cacheId),
                                    redisService.deleteReactive(cacheCard)).asTuple()
                                    .map(t -> ApiResponse.success("Update saldo balance success",
                                            SaldoResponse.from(updatedSaldo)));
                        });
            }
            return saldoQueryRepository.findByCardNumber(request.getCardNumber())
                    .chain(saldo -> {
                        if (saldo == null) {
                            logger.error("Saldo not found for card {}", request.getCardNumber());
                            throw new ResourceNotFoundException("Saldo not found");
                        }

                        saldo.setTotalBalance(request.getTotalBalance().intValue());
                        saldo.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return saldoCommandRepository.persist(saldo)
                                .chain(savedSaldo -> {
                                    String cacheId = "saldo:id:" + savedSaldo.getSaldoId();
                                    String cacheCard = "saldo:card:" + request.getCardNumber();

                                    return Uni.combine().all().unis(
                                            redisService.deleteReactive(cacheId),
                                            redisService.deleteReactive(cacheCard)).asTuple()
                                            .map(t -> ApiResponse.success("Update saldo balance success",
                                                    SaldoResponse.from(savedSaldo)));
                                });
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<SaldoResponse>> updateSaldoWithdraw(
            com.sanedge.saldo.domain.requests.UpdateSaldoWithdraw request) {
        Attributes attrs = Attributes.builder().put("cardNumber", request.getCardNumber()).build();
        logger.info("Updating saldo withdraw for card_number={}", request.getCardNumber());

        return tracingMetrics.traceAndMeasure("updateSaldoWithdraw", "update_saldo_withdraw", attrs, () -> {
            if (request.getDeltaBalance() != null) {
                return saldoCommandRepository.updateBalanceAndWithdrawByDelta(request.getCardNumber(),
                                request.getDeltaBalance(), request.getMinimumBalance(), request.getWithdrawAmount(),
                                request.getWithdrawTime() == null ? null
                                        : java.sql.Timestamp.valueOf(request.getWithdrawTime()), request.getOperationKey())
                        .chain(updated -> {
                            if (updated == 0) {
                                return saldoQueryRepository.findByCardNumber(request.getCardNumber())
                                        .chain(existing -> existing == null
                                                ? Uni.createFrom().failure(new ResourceNotFoundException("Saldo not found"))
                                                : Uni.createFrom().failure(new IllegalStateException(
                                                        "Insufficient balance for requested saldo mutation")));
                            }
                            return saldoQueryRepository.findByCardNumber(request.getCardNumber());
                        })
                        .chain(updatedSaldo -> {
                            String cacheId = "saldo:id:" + updatedSaldo.getSaldoId();
                            String cacheCard = "saldo:card:" + request.getCardNumber();
                            return Uni.combine().all().unis(
                                    redisService.deleteReactive(cacheId),
                                    redisService.deleteReactive(cacheCard)).asTuple()
                                    .map(t -> ApiResponse.success("Update saldo withdraw success",
                                            SaldoResponse.from(updatedSaldo)));
                        });
            }
            return saldoQueryRepository.findByCardNumber(request.getCardNumber())
                    .chain(saldo -> {
                        if (saldo == null) {
                            logger.error("Saldo not found for card {}", request.getCardNumber());
                            throw new ResourceNotFoundException("Saldo not found");
                        }

                        saldo.setTotalBalance(request.getTotalBalance().intValue());
                        saldo.setWithdrawAmount(request.getWithdrawAmount().intValue());
                        if (request.getWithdrawTime() != null) {
                            saldo.setWithdrawTime(java.sql.Timestamp.valueOf(request.getWithdrawTime()));
                        } else {
                            saldo.setWithdrawTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                        }
                        saldo.setUpdatedAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

                        return saldoCommandRepository.persist(saldo)
                                .chain(savedSaldo -> {
                                    String cacheId = "saldo:id:" + savedSaldo.getSaldoId();
                                    String cacheCard = "saldo:card:" + request.getCardNumber();

                                    return Uni.combine().all().unis(
                                            redisService.deleteReactive(cacheId),
                                            redisService.deleteReactive(cacheCard)).asTuple()
                                            .map(t -> ApiResponse.success("Update saldo withdraw success",
                                                    SaldoResponse.from(savedSaldo)));
                                });
                    });
        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
    }
}