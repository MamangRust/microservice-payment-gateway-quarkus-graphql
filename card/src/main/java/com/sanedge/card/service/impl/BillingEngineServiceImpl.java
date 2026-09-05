package com.sanedge.card.service.impl;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.domain.response.BillingStatementResponse;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.card.entity.BillingStatement;
import com.sanedge.card.repository.BillingStatementRepository;
import com.sanedge.card.service.BillingEngineService;
import com.sanedge.card.service.KafkaService;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BillingEngineServiceImpl implements BillingEngineService {

    private static final Logger logger = LoggerFactory.getLogger(BillingEngineServiceImpl.class);

    @Inject
    BillingStatementRepository billingRepo;

    @Inject
    KafkaService kafkaService;

    @Inject
    TracingMetrics tracingMetrics;

    @Override
    @WithTransaction
    public Uni<ApiResponse<Integer>> triggerBillingCycle(int billingCycleDay) {
        Attributes attrs = Attributes.builder()
                .put("billing.cycle.day", String.valueOf(billingCycleDay))
                .build();

        return tracingMetrics.traceAndMeasure("triggerBillingCycle", "billing_trigger", attrs, () -> {
            return billingRepo.findByBillingCycleDay(billingCycleDay)
                    .chain(openStatements -> {
                        if (openStatements.isEmpty()) {
                            logger.info("No open statements found for billing cycle day {}", billingCycleDay);
                            return Uni.createFrom().item(new ApiResponse<>("success",
                                    "No open statements to process", 0));
                        }

                        List<BillingStatement> updated = openStatements.stream()
                                .map(statement -> {
                                    statement.closingBalance = statement.closingBalance != null
                                            ? statement.closingBalance : BigDecimal.ZERO;
                                    statement.minimumPayment = statement.closingBalance.multiply(
                                            new BigDecimal("0.1"));
                                    statement.status = "CLOSED";
                                    return statement;
                                })
                                .collect(Collectors.toList());

                        int count = updated.size();
                        return billingRepo.persist(updated)
                                .chain(saved -> {
                                    JsonObject event = new JsonObject()
                                            .put("billingCycleDay", billingCycleDay)
                                            .put("statementsProcessed", count);
                                    return kafkaService.sendMessage("card.statement.generated",
                                            String.valueOf(billingCycleDay), event)
                                            .map(v -> count);
                                })
                                .onFailure().recoverWithItem(e -> {
                                    logger.error("Failed to persist billing statements", e);
                                    return count;
                                })
                                .map(c -> new ApiResponse<>("success",
                                        "Billing cycle processed for " + c + " statements", c));
                    });
        }).onFailure().recoverWithItem(e -> {
            logger.error("Billing cycle trigger failed for day {}", billingCycleDay, e);
            return new ApiResponse<>("error", "Billing cycle trigger failed: " + e.getMessage(), 0);
        });
    }

    @Override
    public Uni<ApiResponse<BillingStatementResponse>> getStatement(String cardNumber, LocalDate statementDate) {
        return billingRepo.findByCardNumberAndStatementDate(cardNumber, Date.valueOf(statementDate))
                .map(bs -> {
                    if (bs == null) {
                        return new ApiResponse<BillingStatementResponse>("error", "Statement not found", null);
                    }
                    return new ApiResponse<>("success", "Statement found", BillingStatementResponse.from(bs));
                })
                .onFailure().recoverWithItem(e -> {
                    logger.error("Failed to get statement for card={}, date={}", cardNumber, statementDate, e);
                    return new ApiResponse<>("error", "Failed to get statement", null);
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<BillingStatementResponse>>> getStatementsByCard(
            String cardNumber, int page, int size) {
        Uni<List<BillingStatement>> listUni = billingRepo.findByCardNumber(cardNumber, page, size);
        Uni<Long> countUni = billingRepo.countByCardNumber(cardNumber);

        return Uni.combine().all().unis(listUni, countUni).asTuple()
                .map(tuple -> {
                    List<BillingStatement> statements = tuple.getItem1();
                    Long totalRecords = tuple.getItem2();

                    int totalPages = (int) Math.ceil((double) totalRecords / size);
                    PaginationMeta pagination = new PaginationMeta(page, size, totalPages,
                            totalRecords.intValue());

                    List<BillingStatementResponse> data = statements.stream()
                            .map(BillingStatementResponse::from)
                            .collect(Collectors.toList());

                    return new ApiResponsePagination<>("success", "Statements retrieved", data, pagination);
                })
                .onFailure().recoverWithItem(e -> {
                    logger.error("Failed to get statements for card={}", cardNumber, e);
                    return new ApiResponsePagination<>("error", "Failed to get statements",
                            List.of(), new PaginationMeta(page, size, 0, 0));
                });
    }
}
