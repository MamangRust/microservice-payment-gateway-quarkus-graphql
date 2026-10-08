package com.sanedge.transaction.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.adapter.card.CardPort;
import com.sanedge.common.adapter.model.Card;
import com.sanedge.common.adapter.model.Merchant;
import com.sanedge.common.adapter.merchant.MerchantPort;
import com.sanedge.common.adapter.saldo.SaldoPort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.enums.Status;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.RequestFingerprint;
import com.sanedge.transaction.domain.requests.CreateTransactionRequest;
import com.sanedge.transaction.domain.requests.UpdateTransactionRequest;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.transaction.entity.Outbox;
import com.sanedge.transaction.entity.Transaction;
import com.sanedge.transaction.repository.OutboxRepository;
import com.sanedge.transaction.repository.TransactionCommandRepository;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.KafkaService;
import com.sanedge.transaction.service.TransactionCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

@ApplicationScoped
public class TransactionCommandServiceImpl implements TransactionCommandService {
        private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);

        private final TransactionQueryRepository transactionQueryRepository;
        private final TransactionCommandRepository transactionCommandRepository;
        private final MerchantPort merchantPort;
        private final SaldoPort saldoPort;
        private final CardPort cardPort;
        private final Validator validator;
        private final RedisService redisService;
        private final KafkaService kafkaService;
        private final TracingMetrics tracingMetrics;
        private final OutboxRepository outboxRepository;

        @Inject
        public TransactionCommandServiceImpl(TransactionQueryRepository transactionQueryRepository,
                        TransactionCommandRepository transactionCommandRepository,
                        MerchantPort merchantPort,
                        SaldoPort saldoPort,
                        CardPort cardPort,
                        Validator validator,
                        RedisService redisService,
                        KafkaService kafkaService,
                        TracingMetrics tracingMetrics,
                        OutboxRepository outboxRepository) {
                this.transactionQueryRepository = transactionQueryRepository;
                this.transactionCommandRepository = transactionCommandRepository;
                this.merchantPort = merchantPort;
                this.saldoPort = saldoPort;
                this.cardPort = cardPort;
                this.validator = validator;
                this.redisService = redisService;
                this.kafkaService = kafkaService;
                this.tracingMetrics = tracingMetrics;
                this.outboxRepository = outboxRepository;
        }

        /**
         * Writes a transaction.created stats event into the transactional outbox
         * (same DB transaction as the transaction persist). Relayed to Kafka by
         * {@code OutboxPublisher}.
         */
        private Uni<Void> persistOutboxEvent(Transaction transaction) {
                Outbox outbox = new Outbox();
                outbox.setDomain("transaction");
                outbox.setTopic("stats.payment.transaction.event");
                outbox.setEventKey(String.valueOf(transaction.getTransactionId()));
                outbox.setEventId(java.util.UUID.randomUUID().toString());
                JsonObject payload = new JsonObject()
                                .put("transaction_id", transaction.getTransactionId())
                                .put("transaction_no", transaction.getTransactionNo() != null
                                                ? transaction.getTransactionNo().toString() : null)
                                .put("card_number", transaction.getCardNumber())
                                .put("merchant_id", transaction.getMerchantId())
                                .put("amount", transaction.getAmount())
                                .put("payment_method", transaction.getPaymentMethod())
                                .put("status", transaction.getStatus() != null ? transaction.getStatus().name() : null);
                outbox.setPayload(com.sanedge.common.event.EventEnvelope
                                .withDefaults(payload, "transaction.created")
                                .encode());
                return outboxRepository.persist(outbox).replaceWithVoid();
        }

        private Uni<Void> evictCaches(String cardNum, String merchantCardNum, Long merchantId, Long transactionId) {
                String key1 = "saldo:card:" + cardNum;
                String key2 = merchantCardNum != null ? "saldo:card:" + merchantCardNum : null;
                String key3 = "transactions:id:" + transactionId;
                String key4 = merchantId != null ? "transactions:merchant:" + merchantId : null;

                if (key2 != null) {
                        return Uni.combine().all().unis(
                                        redisService.deleteReactive(key1),
                                        redisService.deleteReactive(key2),
                                        redisService.deleteReactive(key3),
                                        key4 != null ? redisService.deleteReactive(key4) : Uni.createFrom().voidItem())
                                        .discardItems();
                } else {
                        return Uni.combine().all().unis(
                                        redisService.deleteReactive(key1),
                                        redisService.deleteReactive(key3),
                                        key4 != null ? redisService.deleteReactive(key4) : Uni.createFrom().voidItem())
                                        .discardItems();
                }
        }

        private String saldoOperationKey(String prefix, String idempotencyKey, String suffix) {
                String key = (idempotencyKey == null || idempotencyKey.isBlank())
                                ? java.util.UUID.randomUUID().toString() : idempotencyKey;
                return suffix == null ? prefix + ":" + key : prefix + ":" + key + ":" + suffix;
        }

        private <T> void validateRequest(T req) {
                Set<ConstraintViolation<T>> violations = validator.validate(req);
                if (!violations.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (ConstraintViolation<T> violation : violations) {
                                sb.append(violation.getPropertyPath()).append(": ").append(violation.getMessage())
                                                .append("; ");
                        }
                        logger.error("Validation failed: {}", sb.toString());
                        throw new ConstraintViolationException("Validation failed: " + sb, violations);
                }
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponse>> create(String apiKey, CreateTransactionRequest req) {
                String key = req.getIdempotencyKey();
                if (key == null || key.isBlank()) {
                        return createInternal(apiKey, req);
                }
                return transactionQueryRepository.lockIdempotencyKey(key)
                                .chain(v -> transactionQueryRepository.findByIdempotencyKey(key))
                                .chain(existing -> {
                                        if (existing == null) {
                                                return createInternal(apiKey, req);
                                        }
                                        if (!sameRequest(existing, req)) {
                                                return Uni.createFrom().failure(new ResourceAlreadyExistsException(
                                                                "Idempotency key is already used for a different transaction request"));
                                        }
                                        return Uni.createFrom().item(ApiResponse.success(
                                                        "Transaction request already processed",
                                                        TransactionResponse.from(existing)));
                                });
        }

        private boolean sameRequest(Transaction existing, CreateTransactionRequest req) {
                return existing.getRequestFingerprint() == null
                                ? existing.getCardNumber().equals(req.getCardNumber())
                                    && existing.getAmount().longValue() == req.getAmount()
                                    && existing.getPaymentMethod().equals(req.getPaymentMethod())
                                    && existing.getMerchantId().longValue() == req.getMerchantId()
                                : existing.getRequestFingerprint().equals(RequestFingerprint.sha256(
                                    req.getCardNumber(), String.valueOf(req.getAmount()), req.getPaymentMethod(),
                                    String.valueOf(req.getMerchantId())));
        }

                private Uni<ApiResponse<TransactionResponse>> createInternal(String apiKey, CreateTransactionRequest req) {
                try {
                        validateRequest(req);
                } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                }

                Attributes attrs = Attributes.builder().put("cardNumber", req.getCardNumber()).build();
                logger.info("Starting CreateTransaction process, apiKey={}, req={}", apiKey, req);
                final Transaction[] ledgerRef = new Transaction[1];

                return tracingMetrics.traceAndMeasure("createTransaction", "create", attrs, () -> {
                        return merchantPort
                                        .findByApiKey(apiKey)
                                        .chain(merchant -> cardPort.findUserCardByCardNumber(req.getCardNumber())
                                                        .chain(card -> saldoPort
                                                                        .findByCardNumber(req.getCardNumber())
                                                                        .chain(saldo -> {
                                                                                if (saldo.totalBalance() < req.getAmount()) {
                                                                                        logger.error("Insufficient balance, requested: {}, available: {}",
                                                                                                        req.getAmount(),
                                                                                                        saldo.totalBalance());
                                                                                        return Uni.createFrom()
                                                                                                        .failure(new ResourceNotFoundException(
                                                                                                                        "Insufficient balance"));
                                                                                }

                                                                                Long updatedSaldo = (long) saldo.totalBalance()
                                                                                                - req.getAmount();

                                                                                Transaction transactionEntity = new Transaction();
                                                                                UUID transactionNo = UUID.randomUUID();

                                                                                transactionEntity.setCardNumber(req.getCardNumber());
                                                                                transactionEntity.setMerchantId(req.getMerchantId().intValue());
                                                                                transactionEntity.setAmount(req.getAmount().intValue());
                                                                                transactionEntity.setPaymentMethod(req.getPaymentMethod());
                                                                                transactionEntity.setTransactionTime(Timestamp.valueOf(LocalDateTime.now()));
                                                                                transactionEntity.setTransactionNo(transactionNo);
                                                                                transactionEntity.setRequestFingerprint(RequestFingerprint.sha256(
                                                                                                req.getCardNumber(), String.valueOf(req.getAmount()),
                                                                                                req.getPaymentMethod(), String.valueOf(req.getMerchantId())));
                                                                                transactionEntity.setIdempotencyKey(req.getIdempotencyKey() == null
                                                                                                || req.getIdempotencyKey().isBlank() ? null
                                                                                                                : req.getIdempotencyKey());
                                                                                transactionEntity.setStatus(Status.PENDING);
                                                                                transactionEntity.setCompensationLegACard(card.cardNumber());
                                                                                transactionEntity.setCompensationLegADelta(-req.getAmount().intValue());
                                                                                ledgerRef[0] = transactionEntity;

                                                                                return saldoPort
                                                                                                .updateSaldoBalance(new SaldoPort.BalanceUpdate(
                                                                                                                card.cardNumber(),
                                                                                                                updatedSaldo.intValue(),
                                                                                                                -req.getAmount().intValue(),
                                                                                                                0, null, null,
                                                                                                                saldoOperationKey("txn", req.getIdempotencyKey(), null)))
                                                                                                .chain(v -> {
                                                                                                        transactionEntity.setCompensationLegAApplied(true);
                                                                                                        return transactionCommandRepository
                                                                                                                        .persist(transactionEntity);
                                                                                                })
                                                                                                .chain(persistedTx -> transactionCommandRepository
                                                                                                                .updateTransactionStatus(
                                                                                                                                persistedTx.getTransactionId(),
                                                                                                                                Status.SUCCESS.toString())
                                                                                                                .chain(updatedTx -> persistOutboxEvent(updatedTx)
                                                                                                                                .replaceWith(updatedTx))
                                                                                                                .chain(updatedTx -> creditMerchantBalance(
                                                                                                                                transactionEntity, merchant, card,
                                                                                                                                req, updatedTx)));
                                                                        })));
                }).onFailure().recoverWithUni(e -> {
                        if (ledgerRef[0] != null && ledgerRef[0].getTransactionId() != null) {
                                return transactionCommandRepository
                                                .markCompensationRequired(ledgerRef[0].getTransactionId(), e.getMessage())
                                                .map(ignored -> new ApiResponse<>("error",
                                                                "Transaction requires reconciliation: " + e.getMessage(), null));
                        }
                        return Uni.createFrom().item(new ApiResponse<>("error",
                                        "Error in create transaction: " + e.getMessage(), null));
                });
        }

        private Uni<ApiResponse<TransactionResponse>> creditMerchantBalance(
                        Transaction transactionEntity, Merchant merchant, Card card,
                        CreateTransactionRequest req, Transaction updatedTx) {
                return cardPort
                                .findCardByUserId(merchant.userId())
                                .chain(merchantCard -> saldoPort
                                                .findByCardNumber(merchantCard.cardNumber())
                                                .chain(merchantSaldo -> {
                                                        Long updatedMerchantSaldo = (long) merchantSaldo
                                                                        .totalBalance()
                                                                        + req.getAmount();
                                                        return saldoPort
                                                                        .updateSaldoBalance(new SaldoPort.BalanceUpdate(
                                                                                        merchantCard.cardNumber(),
                                                                                        updatedMerchantSaldo.intValue(),
                                                                                        req.getAmount().intValue(),
                                                                                        0, null, null,
                                                                                        saldoOperationKey("txn", req.getIdempotencyKey(), "merchant")))
                                                                        .chain(v2b -> {
                                                                                transactionEntity.setCompensationLegBCard(merchantCard.cardNumber());
                                                                                transactionEntity.setCompensationLegBDelta(req.getAmount().intValue());
                                                                                transactionEntity.setCompensationLegBApplied(true);
                                                                                return transactionCommandRepository
                                                                                                .persist(transactionEntity)
                                                                                                .replaceWith(v2b);
                                                                        })
                                                                        .chain(v2 -> evictCaches(
                                                                                        req.getCardNumber(),
                                                                                        merchantCard.cardNumber(),
                                                                                        (long) merchant.id(),
                                                                                        updatedTx.getTransactionId()))
                                                                        .chain(v3 -> {
                                                                                if (card.email() != null && !card.email().isEmpty()) {
                                                                                        String emailSubject = "Transaction Successful - SanEdge";
                                                                                        String emailBody = String.format(
                                                                                                        "Hello,\n\nYour transaction of %d has been processed successfully.\n\nRegards,\nSupport Team",
                                                                                                        req.getAmount().intValue());

                                                                                        JsonObject emailPayload = new JsonObject()
                                                                                                        .put("email", card.email())
                                                                                                        .put("subject", emailSubject)
                                                                                                        .put("body", emailBody);
                                                                                        kafkaService
                                                                                                        .sendMessage("email-service-topic-transaction-create",
                                                                                                                        String.valueOf(updatedTx.getTransactionId()),
                                                                                                                        emailPayload)
                                                                                                        .onFailure().invoke(e -> logger.warn(
                                                                                                                        "Kafka email failed for txn {}: {}",
                                                                                                                        updatedTx.getTransactionId(),
                                                                                                                        e.getMessage()))
                                                                                                        .subscribe().with(v -> {}, e -> {});
                                                                                }

                                                                                TransactionResponse response = TransactionResponse
                                                                                                .from(updatedTx);
                                                                                logger.info("CreateTransaction completed, transaction_id={}",
                                                                                                response.getId());

                                                                                return Uni.createFrom().item(ApiResponse
                                                                                                .success("Transaction created successfully",
                                                                                                                response));
                                                                        });
                                                }));
        }


        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponse>> update(String apiKey, UpdateTransactionRequest req) {
                try {
                        validateRequest(req);
                } catch (Exception e) {
                        return Uni.createFrom().item(new ApiResponse<>("error", e.getMessage(), null));
                }

                Long transactionId = req.getTransactionId();
                if (transactionId == null) {
                        return Uni.createFrom().item(new ApiResponse<>("error", "transaction_id is required", null));
                }

                Attributes attrs = Attributes.builder().put("transactionId", transactionId).build();
                logger.info("Starting UpdateTransaction process: {}", req);

                return tracingMetrics.traceAndMeasure("updateTransaction", "update", attrs, () -> {
                        return transactionQueryRepository.findTransactionById(transactionId)
                                        .chain(transaction -> {
                                                if (transaction == null) {
                                                        return Uni.createFrom().failure(new ResourceNotFoundException(
                                                                        "Transaction " + transactionId + " not found"));
                                                }
                                                return merchantPort
                                                                .findByApiKey(apiKey)
                                                                .chain(merchant -> {
                                                                        if (!transaction.getMerchantId()
                                                                                        .equals(merchant.id())) {
                                                                                logger.error("Unauthorized access to transaction {}",
                                                                                                transactionId);
                                                                                return transactionCommandRepository
                                                                                                .updateTransactionStatus(
                                                                                                                transactionId,
                                                                                                                Status.FAILED.toString())
                                                                                                .chain(v -> Uni.createFrom()
                                                                                                                .failure(new ResourceNotFoundException(
                                                                                                                                "unauthorized access")));
                                                                        }

                                                                        return cardPort.findCardByCardNumber(
                                                                                        transaction.getCardNumber())
                                                                                        .chain(card -> saldoPort
                                                                                                        .findByCardNumber(card.cardNumber())
                                                                                                        .chain(saldo -> {
                                                                                                                Long restoredBalance = (long) saldo
                                                                                                                                .totalBalance()
                                                                                                                                + transaction.getAmount()
                                                                                                                                                .longValue();

                                                                                                                return saldoPort
                                                                                                                                .updateSaldoBalance(new SaldoPort.BalanceUpdate(
                                                                                                                                                card.cardNumber(),
                                                                                                                                                restoredBalance.intValue(),
                                                                                                                                                null, null, null, null, null))
                                                                                                                                .chain(v1 -> {
                                                                                                                                        if (restoredBalance < req
                                                                                                                                                        .getAmount()) {
                                                                                                                                                logger.error("Insufficient balance after restore, available={}, requested={}",
                                                                                                                                                                restoredBalance,
                                                                                                                                                                req.getAmount());
                                                                                                                                                return transactionCommandRepository
                                                                                                                                                                .updateTransactionStatus(
                                                                                                                                                                                transactionId,
                                                                                                                                                                                Status.FAILED.toString())
                                                                                                                                                                .chain(v2 -> Uni.createFrom()
                                                                                                                                                                                .failure(new ResourceNotFoundException(
                                                                                                                                                                                                "Insufficient balance")));
                                                                                                                                        }

                                                                                                                                        Long updatedBalance = restoredBalance
                                                                                                                                                        - req.getAmount();

                                                                                                                                        transaction.setAmount(
                                                                                                                                                        req.getAmount().intValue());
                                                                                                                                        transaction.setPaymentMethod(
                                                                                                                                                        req.getPaymentMethod());
                                                                                                                                        transaction.setTransactionTime(
                                                                                                                                                        req.getTransactionTime() != null
                                                                                                                                                                        ? Timestamp.valueOf(
                                                                                                                                                                                        req.getTransactionTime())
                                                                                                                                                                        : new java.sql.Timestamp(
                                                                                                                                                                                        System.currentTimeMillis()));

                                                                                                                                        return saldoPort
                                                                                                                                                        .updateSaldoBalance(new SaldoPort.BalanceUpdate(
                                                                                                                                                                        card.cardNumber(),
                                                                                                                                                                        updatedBalance.intValue(),
                                                                                                                                                                        null, null, null, null, null))
                                                                                                                                                        .chain(v3 -> transactionCommandRepository
                                                                                                                                                                        .persist(transaction))
                                                                                                                                                        .chain(v4 -> transactionCommandRepository
                                                                                                                                                                        .updateTransactionStatus(
                                                                                                                                                                                        transactionId,
                                                                                                                                                                                        Status.SUCCESS.toString()))
                                                                                                                                                        .chain(updatedTx -> evictCaches(
                                                                                                                                                                        card.cardNumber(),
                                                                                                                                                                        null,
                                                                                                                                                                        (long) merchant.id(),
                                                                                                                                                                        transactionId)
                                                                                                                                                                        .map(v5 -> {
                                                                                                                                                                                TransactionResponse response = TransactionResponse
                                                                                                                                                                                                .from(updatedTx);
                                                                                                                                                                                logger.info("Transaction {} updated successfully",
                                                                                                                                                                                                transactionId);
                                                                                                                                                                                return ApiResponse
                                                                                                                                                                                                .success("Transaction updated successfully",
                                                                                                                                                                                                                response);
                                                                                                                                                                        }));
                                                                                                                                });
                                                                                                        }));
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error",
                                "Failed to update transaction: " + e.getMessage(), null));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponseDeleteAt>> trashed(Long transactionId) {
                Attributes attrs = Attributes.builder().put("transactionId", transactionId).build();
                logger.info("Trashing transaction id={}", transactionId);

                return tracingMetrics.traceAndMeasure("trashTransaction", "trashed", attrs, () -> {
                        return transactionCommandRepository.trashed(transactionId)
                                        .chain(tx -> {
                                                if (tx == null) {
                                                        return Uni.createFrom().failure(new ResourceNotFoundException(
                                                                        "Transaction not found"));
                                                }
                                                return evictCaches(tx.getCardNumber(), null,
                                                                tx.getMerchantId().longValue(), transactionId)
                                                                .map(v -> ApiResponse.success(
                                                                                "Transaction trashed successfully!",
                                                                                TransactionResponseDeleteAt.from(tx)));
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error",
                                "Failed to trash transaction: " + e.getMessage(), null));
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<TransactionResponseDeleteAt>> restore(Long transactionId) {
                Attributes attrs = Attributes.builder().put("transactionId", transactionId).build();
                logger.info("Restoring transaction id={}", transactionId);

                return tracingMetrics.traceAndMeasure("restoreTransaction", "restore", attrs, () -> {
                        return transactionCommandRepository.restore(transactionId)
                                        .chain(tx -> {
                                                if (tx == null) {
                                                        logger.error("Transaction restore failed - not found or must be trashed first with id {}", transactionId);
                                                        throw new InvalidRequestException("Transaction not found or must be trashed first");
                                                }
                                                return evictCaches(tx.getCardNumber(), null,
                                                                tx.getMerchantId().longValue(), transactionId)
                                                                 .map(v -> ApiResponse.success(
                                                                                 "Transaction restored successfully!",
                                                                                 TransactionResponseDeleteAt.from(tx)));
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deletePermanent(Long transactionId) {
                Attributes attrs = Attributes.builder().put("transactionId", transactionId).build();
                logger.info("Permanently deleting transaction id={}", transactionId);

                return tracingMetrics.traceAndMeasure("deletePermanentTransaction", "delete_permanent", attrs, () -> {
                        return transactionCommandRepository.findById(transactionId)
                                        .chain(tx -> {
                                                if (tx == null || tx.getDeletedAt() == null) {
                                                        logger.error("Permanent delete failed - transaction not found or must be trashed before permanent deletion with id {}", transactionId);
                                                        throw new InvalidRequestException("Transaction not found or must be trashed before permanent deletion");
                                                }
                                                return transactionCommandRepository.deletePermanent(transactionId)
                                                                .chain(success -> evictCaches(tx.getCardNumber(), null,
                                                                                tx.getMerchantId().longValue(),
                                                                                transactionId)
                                                                                 .map(v -> ApiResponse.success(
                                                                                                 "Transaction permanently deleted!",
                                                                                                 true)));
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> restoreAll() {
                logger.info("Restoring ALL trashed transactions");

                return tracingMetrics.traceAndMeasure("restoreAllTransactions", "restore_all", () -> {
                        return transactionCommandRepository.restoreAllDeleted()
                                        .map(success -> {
                                                if (!success) {
                                                        throw new ResourceNotFoundException("No trashed transactions found");
                                                }
                                                return ApiResponse.success("All transactions restored successfully!", true);
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Boolean>> deleteAll() {
                logger.info("Permanently deleting ALL trashed transactions");

                return tracingMetrics.traceAndMeasure("deleteAllTransactions", "delete_all", () -> {
                        return transactionCommandRepository.deleteAllDeleted()
                                        .map(success -> {
                                                if (!success) {
                                                        throw new ResourceNotFoundException("No trashed transactions found");
                                                }
                                                return ApiResponse.success("All transactions permanently deleted!", true);
                                        });
                });
        }
}
