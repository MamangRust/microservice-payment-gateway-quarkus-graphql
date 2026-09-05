package com.sanedge.card.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.domain.requests.PostPaymentRequest;
import com.sanedge.card.domain.response.CardPaymentResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.card.entity.CardPayment;
import com.sanedge.card.repository.CardCommandRepository;
import com.sanedge.card.repository.CardPaymentRepository;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.CardPaymentService;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CardPaymentServiceImpl implements CardPaymentService {

        private static final Logger logger = LoggerFactory.getLogger(CardPaymentServiceImpl.class);

        @Inject
        CardPaymentRepository paymentRepo;

        @Inject
        CardCommandRepository cardCommandRepo;

        @Inject
        CardQueryRepository cardQueryRepository;

        @Inject
        KafkaService kafkaService;

        @Inject
        TracingMetrics tracingMetrics;

        @Override
        @WithTransaction
        public Uni<ApiResponse<CardPaymentResponse>> postPayment(PostPaymentRequest req) {
                Attributes attrs = Attributes.builder()
                                .put("card.number", req.getCardNumber())
                                .put("amount", req.getAmount().toString())
                                .build();

                return tracingMetrics.traceAndMeasure("postPayment", "card_payment", attrs, () -> {
                        return cardQueryRepository.findByCardNumber(req.getCardNumber())
                                        .chain(card -> {
                                                if (card == null) {
                                                        return Uni.createFrom().item(
                                                                        new ApiResponse<CardPaymentResponse>("error",
                                                                                        "Card not found", null));
                                                }

                                                CardPayment payment = new CardPayment();
                                                payment.cardNumber = req.getCardNumber();
                                                payment.statementId = req.getStatementId();
                                                payment.amount = req.getAmount();
                                                payment.paymentChannel = req.getPaymentChannel();
                                                payment.referenceId = req.getReferenceId();
                                                payment.status = "PENDING";
                                                payment.paidAt = Instant.now();

                                                return paymentRepo.persist(payment)
                                                                .chain(savedPayment -> {
                                                                        return paymentRepo.completePayment(
                                                                                        savedPayment.paymentId);
                                                                })
                                                                .chain(completed -> {
                                                                        // Produce Kafka event
                                                                        JsonObject event = new JsonObject()
                                                                                        .put("paymentId",
                                                                                                        completed.paymentId)
                                                                                        .put("cardNumber",
                                                                                                        completed.cardNumber)
                                                                                        .put("amount", completed.amount
                                                                                                        .toString())
                                                                                        .put("paymentChannel",
                                                                                                        completed.paymentChannel)
                                                                                        .put("referenceId",
                                                                                                        completed.referenceId)
                                                                                        .put("status", completed.status);

                                                                        return kafkaService.sendMessage(
                                                                                        "card.payment.posted",
                                                                                        completed.cardNumber, event)
                                                                                        .map(v -> new ApiResponse<>(
                                                                                                        "success",
                                                                                                        "Payment posted successfully",
                                                                                                        CardPaymentResponse
                                                                                                                        .from(completed)))
                                                                                        .onFailure()
                                                                                        .recoverWithItem(e -> {
                                                                                                logger.error("Kafka event failed for payment {}",
                                                                                                                completed.paymentId,
                                                                                                                e);
                                                                                                return new ApiResponse<>(
                                                                                                                "success",
                                                                                                                "Payment posted successfully",
                                                                                                                CardPaymentResponse
                                                                                                                                .from(completed));
                                                                                        });
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> {
                        logger.error("Post payment failed for card={}", req.getCardNumber(), e);
                        return new ApiResponse<>("error", "Payment failed: " + e.getMessage(), null);
                });
        }

        @Override
        public Uni<ApiResponsePagination<List<CardPaymentResponse>>> getPaymentHistory(
                        String cardNumber, int page, int size) {
                Uni<List<CardPayment>> listUni = paymentRepo.findByCardNumber(cardNumber, page, size);
                Uni<Long> countUni = paymentRepo.countByCardNumber(cardNumber);

                return Uni.combine().all().unis(listUni, countUni).asTuple()
                                .map(tuple -> {
                                        List<CardPayment> payments = tuple.getItem1();
                                        Long totalRecords = tuple.getItem2();

                                        int totalPages = (int) Math.ceil((double) totalRecords / size);
                                        PaginationMeta pagination = new PaginationMeta(page, size, totalPages,
                                                        totalRecords.intValue());

                                        List<CardPaymentResponse> data = payments.stream()
                                                        .map(CardPaymentResponse::from)
                                                        .collect(Collectors.toList());

                                        return new ApiResponsePagination<>("success", "Payment history retrieved", data,
                                                        pagination);
                                })
                                .onFailure().recoverWithItem(e -> {
                                        logger.error("Failed to get payment history for card={}", cardNumber, e);
                                        return new ApiResponsePagination<>("error", "Failed to get payment history",
                                                        List.of(), new PaginationMeta(page, size, 0, 0));
                                });
        }

        @Override
        public Uni<ApiResponse<Long>> countPayments(String cardNumber) {
                return paymentRepo.countByCardNumber(cardNumber)
                                .map(count -> new ApiResponse<>("success", "Payment count retrieved", count))
                                .onFailure().recoverWithItem(e -> {
                                        logger.error("Failed to count payments for card={}", cardNumber, e);
                                        return new ApiResponse<>("error", "Failed to count payments", 0L);
                                });
        }
}
