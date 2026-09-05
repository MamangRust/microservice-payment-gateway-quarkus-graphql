package com.sanedge.transaction.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.transaction.domain.requests.FindAllTransactionCardNumber;
import com.sanedge.transaction.domain.requests.FindAllTransactions;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.transaction.domain.response.TransactionResponse;
import com.sanedge.transaction.domain.response.TransactionResponseDeleteAt;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.transaction.repository.TransactionQueryRepository;
import com.sanedge.transaction.service.TransactionQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransactionQueryServiceImpl implements TransactionQueryService {
        private static final Logger logger = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);

        private final TransactionQueryRepository transactionQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long QUERY_CACHE_TTL_SECONDS = 300;

        @Inject
        public TransactionQueryServiceImpl(TransactionQueryRepository transactionQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.transactionQueryRepository = transactionQueryRepository;
                this.redisService = redisService;
                this.objectMapper = objectMapper;
                this.tracingMetrics = tracingMetrics;
        }

        private String toJson(Object obj) {
                try {
                        return objectMapper.writeValueAsString(obj);
                } catch (JsonProcessingException e) {
                        logger.error("Error serializing object to JSON", e);
                        throw new RuntimeException("Failed to serialize object", e);
                }
        }

        private <T> T fromJson(String json, TypeReference<T> typeReference) {
                try {
                        return objectMapper.readValue(json, typeReference);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON with TypeReference", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
                }
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findAll(FindAllTransactions req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:all:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching all transactions | Page: {}, Size: {}, Search: {}", page + 1, pageSize,
                                search.isEmpty() ? "None" : search);

                return tracingMetrics.traceAndMeasure("findAllTransactions", "find_all", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransactionResponse>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transactionQueryRepository
                                                                .findTransactions(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransactionResponse> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransactionResponse::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransactionResponse>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Transactions retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error", "Failed to fetch transactions",
                                Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponse>>> findAllByCardNumber(
                        FindAllTransactionCardNumber req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";
                String cardNumber = req.getCardNumber();

                if (cardNumber == null || cardNumber.trim().isEmpty()) {
                        logger.error("Card number is required");
                        return Uni.createFrom().item(new ApiResponsePagination<>("error", "Card number is required",
                                        Collections.emptyList(), null));
                }

                String cacheKey = String.format("transactions:card-num:%s:%d:%d:%s", cardNumber, page, pageSize,
                                search);
                Attributes attrs = Attributes.builder()
                                .put("cardNumber", cardNumber)
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching transactions by cardNumber={} | Page: {}, Size: {}", cardNumber, page + 1,
                                pageSize);

                return tracingMetrics.traceAndMeasure("findAllTransactionsByCardNumber", "find_all_by_card_number",
                                attrs, () -> {
                                        return redisService.getReactive(cacheKey)
                                                        .chain(cachedJson -> {
                                                                if (cachedJson != null) {
                                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                                        ApiResponsePagination<List<TransactionResponse>> cached = fromJson(
                                                                                        cachedJson,
                                                                                        new TypeReference<ApiResponsePagination<List<TransactionResponse>>>() {
                                                                                        });
                                                                        return Uni.createFrom().item(cached);
                                                                }

                                                                logger.info("Cache MISS for key: {}. Fetching from DB.",
                                                                                cacheKey);
                                                                return transactionQueryRepository
                                                                                .findTransactionsByCardNumber(req)
                                                                                .chain(pagedResult -> {
                                                                                        List<TransactionResponse> data = pagedResult
                                                                                                        .getData()
                                                                                                        .stream()
                                                                                                        .map(TransactionResponse::from)
                                                                                                        .collect(Collectors
                                                                                                                        .toList());

                                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                                        pagedResult.getTotalRecords(),
                                                                                                        page + 1,
                                                                                                        pageSize,
                                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                                        .getTotalRecords()
                                                                                                                        / pageSize));

                                                                                        ApiResponsePagination<List<TransactionResponse>> response = new ApiResponsePagination<>(
                                                                                                        "success",
                                                                                                        "Transactions retrieved successfully",
                                                                                                        data, meta);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> response);
                                                                                });
                                                        });
                                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error",
                                                "Failed to fetch transactions", Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActive(FindAllTransactions req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:active:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching active transactions | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findActiveTransactions", "find_active", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransactionResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transactionQueryRepository
                                                                .findActiveTransactions(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransactionResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransactionResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransactionResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Active transactions retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error", "Failed to fetch transactions",
                                Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashed(FindAllTransactions req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transactions:trashed:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching trashed transactions | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findTrashedTransactions", "find_trashed", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransactionResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransactionResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transactionQueryRepository
                                                                .findTrashedTransactions(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransactionResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransactionResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransactionResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Trashed transactions retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error", "Failed to fetch transactions",
                                Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponse<TransactionResponse>> findById(Long transactionId) {
                String cacheKey = String.format("transactions:id:%d", transactionId);
                Attributes attrs = Attributes.builder().put("transactionId", transactionId).build();

                logger.info("Finding transaction by id={}", transactionId);

                return tracingMetrics.traceAndMeasure("findTransactionById", "find_by_id", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        TransactionResponse cached = fromJson(cachedJson,
                                                                        new TypeReference<TransactionResponse>() {
                                                                        });
                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                        "Transaction retrieved successfully", cached));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transactionQueryRepository.findTransactionById(transactionId)
                                                                .chain(tx -> {
                                                                        if (tx == null) {
                                                                                logger.warn("Transaction not found with id={}",
                                                                                                transactionId);
                                                                                throw new ResourceNotFoundException(
                                                                                                "Transaction not found");
                                                                        }

                                                                        TransactionResponse response = TransactionResponse
                                                                                        .from(tx);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> ApiResponse.success(
                                                                                                        "Transaction retrieved successfully",
                                                                                                        response));
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
        }

        @Override
        public Uni<ApiResponse<List<TransactionResponse>>> findByMerchantId(Long merchantId) {
                String cacheKey = String.format("transactions:merchant:%d", merchantId);
                Attributes attrs = Attributes.builder().put("merchantId", merchantId).build();

                logger.info("Finding transactions by merchantId={}", merchantId);

                return tracingMetrics
                                .traceAndMeasure("findTransactionsByMerchantId", "find_by_merchant_id", attrs, () -> {
                                        return redisService.getReactive(cacheKey)
                                                        .chain(cachedJson -> {
                                                                if (cachedJson != null) {
                                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                                        List<TransactionResponse> cached = fromJson(
                                                                                        cachedJson,
                                                                                        new TypeReference<List<TransactionResponse>>() {
                                                                                        });
                                                                        return Uni.createFrom().item(ApiResponse
                                                                                        .success("Transactions retrieved successfully by merchant id",
                                                                                                        cached));
                                                                }

                                                                logger.info("Cache MISS for key: {}. Fetching from DB.",
                                                                                cacheKey);
                                                                return transactionQueryRepository
                                                                                .findTransactionsByMerchantId(
                                                                                                merchantId)
                                                                                .chain(transactions -> {
                                                                                        List<TransactionResponse> data = transactions
                                                                                                        .stream()
                                                                                                        .map(TransactionResponse::from)
                                                                                                        .collect(Collectors
                                                                                                                        .toList());

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(data),
                                                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> ApiResponse
                                                                                                                        .success("Transactions retrieved successfully by merchant id",
                                                                                                                                        data));
                                                                                });
                                                        });
                                }).onFailure()
                                .recoverWithItem(e -> new ApiResponse<>("error",
                                                "Failed to fetch transactions by merchant id",
                                                Collections.emptyList()));
        }
}