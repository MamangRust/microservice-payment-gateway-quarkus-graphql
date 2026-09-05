package com.sanedge.card.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.domain.requests.AuthorizeCardRequest;
import com.sanedge.card.domain.requests.ReverseTransactionRequest;
import com.sanedge.card.domain.response.CardAuthTransactionResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.card.entity.CardAuthTransaction;
import com.sanedge.card.entity.CardAuthTransaction.AuthTxnStatus;
import com.sanedge.card.entity.Card.CardStatus;
import com.sanedge.card.repository.CardAuthTransactionRepository;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.CardAuthService;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

@ApplicationScoped
public class CardAuthServiceImpl implements CardAuthService {

    private static final Logger logger = LoggerFactory.getLogger(CardAuthServiceImpl.class);

    @Inject
    CardAuthTransactionRepository authTxnRepo;

    @Inject
    CardCommandRepository cardCommandRepo;

    @Inject
    CardQueryRepository cardQueryRepository;

    @Inject
    KafkaService kafkaService;

    @Inject
    TracingMetrics tracingMetrics;

    @Inject
    Validator validator;

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardAuthTransactionResponse>> authorize(AuthorizeCardRequest req) {
        Attributes attrs = Attributes.builder()
                .put("card.number", req.getCardNumber())
                .put("merchant.id", String.valueOf(req.getMerchantId()))
                .build();

        return tracingMetrics.<ApiResponse<CardAuthTransactionResponse>>traceAndMeasure(
                "authorize", "card_authorize", attrs, () -> {

                    return authTxnRepo.findByIdempotencyKey(req.getIdempotencyKey())
                            .chain(existing -> {
                                if (existing != null) {
                                    logger.warn("Duplicate idempotency key: {}", req.getIdempotencyKey());
                                    return Uni.createFrom().item(new ApiResponse<CardAuthTransactionResponse>(
                                            "error",
                                            "Duplicate idempotency key",
                                            CardAuthTransactionResponse.from(existing)));
                                }

                                return cardQueryRepository.findByCardNumber(req.getCardNumber())
                                        .chain(card -> {
                                            if (card == null) {
                                                return Uni.createFrom().item(
                                                        new ApiResponse<CardAuthTransactionResponse>(
                                                                "error",
                                                                "Card not found",
                                                                null));
                                            }
                                            if (card.status != CardStatus.ACTIVE) {
                                                return Uni.createFrom().item(
                                                        new ApiResponse<CardAuthTransactionResponse>(
                                                                "error",
                                                                "Card is not active",
                                                                null));
                                            }

                                            if (card.creditLimit.compareTo(req.getAmount()) < 0) {
                                                return Uni.createFrom().item(
                                                        new ApiResponse<CardAuthTransactionResponse>(
                                                                "error",
                                                                "Insufficient credit limit",
                                                                null));
                                            }

                                            // 4. Create PENDING transaction
                                            CardAuthTransaction txn = new CardAuthTransaction();
                                            txn.cardNumber = req.getCardNumber();
                                            txn.merchantId = req.getMerchantId();
                                            txn.amount = req.getAmount();
                                            txn.currency = req.getCurrency() != null ? req.getCurrency() : "IDR";
                                            txn.posEntryMode = req.getPosEntryMode();
                                            txn.mcc = req.getMcc();
                                            txn.idempotencyKey = req.getIdempotencyKey();
                                            txn.status = AuthTxnStatus.PENDING;
                                            txn.riskScore = computeRiskScore(req.getAmount(), req.getMcc());

                                            return authTxnRepo.persist(txn)
                                                    .chain(savedTxn -> {
                                                        // 5. Auto-approve/decline based on risk score
                                                        if (savedTxn.riskScore > 70) {
                                                            return authTxnRepo.updateStatus(savedTxn.authTxnId,
                                                                    AuthTxnStatus.DECLINED)
                                                                    .chain(declined -> {
                                                                        // Block card for high risk
                                                                        return cardCommandRepo.toggleStatus(
                                                                                card.getCardId())
                                                                                .map(v -> declined);
                                                                    });
                                                        } else {
                                                            return authTxnRepo.updateStatus(savedTxn.authTxnId,
                                                                    AuthTxnStatus.APPROVED);
                                                        }
                                                    })
                                                    .chain(finalTxn -> {
                                                        // 6. Produce Kafka event
                                                        JsonObject event = new JsonObject()
                                                                .put("authTxnId", finalTxn.authTxnId)
                                                                .put("cardNumber", finalTxn.cardNumber)
                                                                .put("merchantId", finalTxn.merchantId)
                                                                .put("amount", finalTxn.amount.toString())
                                                                .put("currency", finalTxn.currency)
                                                                .put("mcc", finalTxn.mcc)
                                                                .put("riskScore", finalTxn.riskScore)
                                                                .put("status", finalTxn.status.name());

                                                        return kafkaService.sendMessage("card.txn.created",
                                                                finalTxn.cardNumber, event)
                                                                .map(v -> new ApiResponse<CardAuthTransactionResponse>(
                                                                        "success",
                                                                        "Authorization "
                                                                                + finalTxn.status.name().toLowerCase(),
                                                                        CardAuthTransactionResponse.from(finalTxn)))
                                                                .onFailure().recoverWithItem(e -> {
                                                                    logger.error(
                                                                            "Failed to send Kafka event for auth txn {}",
                                                                            finalTxn.authTxnId, e);
                                                                    return new ApiResponse<CardAuthTransactionResponse>(
                                                                            "success",
                                                                            "Authorization "
                                                                                    + finalTxn.status.name()
                                                                                            .toLowerCase(),
                                                                            CardAuthTransactionResponse.from(finalTxn));
                                                                });
                                                    });
                                        });
                            });
                }).onFailure().recoverWithItem(e -> {
                    logger.error("Authorization failed for card={}", req.getCardNumber(), e);
                    return new ApiResponse<CardAuthTransactionResponse>("error",
                            "Authorization failed: " + e.getMessage(), null);
                });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<CardAuthTransactionResponse>> reverse(ReverseTransactionRequest req) {
        Attributes attrs = Attributes.builder()
                .put("auth.txn.id", String.valueOf(req.getAuthTxnId()))
                .put("card.number", req.getCardNumber())
                .build();

        return tracingMetrics.<ApiResponse<CardAuthTransactionResponse>>traceAndMeasure(
                "reverse", "card_reverse", attrs, () -> {

                    return authTxnRepo.findById(req.getAuthTxnId())
                            .chain(txn -> {
                                if (txn == null) {
                                    return Uni.createFrom().item(
                                            new ApiResponse<CardAuthTransactionResponse>(
                                                    "error",
                                                    "Transaction not found",
                                                    null));
                                }
                                if (txn.status != AuthTxnStatus.APPROVED) {
                                    return Uni.createFrom().item(
                                            new ApiResponse<CardAuthTransactionResponse>(
                                                    "error",
                                                    "Only approved transactions can be reversed",
                                                    null));
                                }
                                return authTxnRepo.updateStatus(txn.authTxnId, AuthTxnStatus.REVERSED)
                                        .map(reversed -> new ApiResponse<CardAuthTransactionResponse>(
                                                "success",
                                                "Transaction reversed successfully",
                                                CardAuthTransactionResponse.from(reversed)));
                            });
                }).onFailure().recoverWithItem(e -> {
                    logger.error("Reverse failed for txnId={}", req.getAuthTxnId(), e);
                    return new ApiResponse<CardAuthTransactionResponse>("error",
                            "Reverse failed: " + e.getMessage(), null);
                });
    }

    private int computeRiskScore(java.math.BigDecimal amount, String mcc) {
        int score = 0;
        java.math.BigDecimal tenMillion = new java.math.BigDecimal("10000000");
        java.math.BigDecimal fiveMillion = new java.math.BigDecimal("5000000");
        java.math.BigDecimal oneMillion = new java.math.BigDecimal("1000000");

        if (amount.compareTo(tenMillion) > 0) {
            score += 50;
        } else if (amount.compareTo(fiveMillion) > 0) {
            score += 30;
        } else if (amount.compareTo(oneMillion) > 0) {
            score += 15;
        }

        Set<String> blacklistedMcc = Set.of("7995", "5967", "7273", "4829");
        if (mcc != null && blacklistedMcc.contains(mcc)) {
            score += 40;
        }

        return Math.min(score, 100);
    }
}