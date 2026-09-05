package com.sanedge.withdraw.service.impl;

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
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.domain.response.WithdrawResponse;
import com.sanedge.withdraw.domain.response.WithdrawResponseDeleteAt;
import com.sanedge.withdraw.repository.WithdrawQueryRepository;
import com.sanedge.withdraw.service.WithdrawQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WithdrawQueryServiceImpl implements WithdrawQueryService {
        private static final Logger logger = LoggerFactory.getLogger(WithdrawQueryServiceImpl.class);

        private final WithdrawQueryRepository withdrawQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long QUERY_CACHE_TTL_SECONDS = 300;

        @Inject
        public WithdrawQueryServiceImpl(WithdrawQueryRepository withdrawQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.withdrawQueryRepository = withdrawQueryRepository;
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
        public Uni<ApiResponsePagination<List<WithdrawResponse>>> findAll(FindAllWithdraws req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("withdraws:all:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching all withdraws | Page: {}, Size: {}, Search: {}", page + 1, pageSize,
                                search.isEmpty() ? "None" : search);

                return tracingMetrics.traceAndMeasure("findAllWithdraws", "find_all", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<WithdrawResponse>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<WithdrawResponse>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return withdrawQueryRepository.findAllWithdraws(req)
                                                                .chain(pagedResult -> {
                                                                        List<WithdrawResponse> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(WithdrawResponse::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<WithdrawResponse>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Withdraws retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error", "Failed to fetch withdraws",
                                Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<WithdrawResponse>>> findAllByCardNumber(FindAllWithdrawCardNumber req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";
                String cardNumber = req.getCardNumber();

                String cacheKey = String.format("withdraws:card:%s:%d:%d:%s", cardNumber, page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("cardNumber", cardNumber)
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching withdraws by card number={} | Page: {}, Size: {} | Search {}", cardNumber,
                                page + 1, pageSize, search);

                return tracingMetrics.traceAndMeasure("findAllWithdrawsByCardNumber", "find_all_by_card_number", attrs,
                                () -> {
                                        return redisService.getReactive(cacheKey)
                                                        .chain(cachedJson -> {
                                                                if (cachedJson != null) {
                                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                                        ApiResponsePagination<List<WithdrawResponse>> cached = fromJson(
                                                                                        cachedJson,
                                                                                        new TypeReference<ApiResponsePagination<List<WithdrawResponse>>>() {
                                                                                        });
                                                                        return Uni.createFrom().item(cached);
                                                                }

                                                                logger.info("Cache MISS for key: {}. Fetching from DB.",
                                                                                cacheKey);
                                                                return withdrawQueryRepository
                                                                                .findAllByCardNumber(req)
                                                                                .chain(pagedResult -> {
                                                                                        List<WithdrawResponse> data = pagedResult
                                                                                                        .getData()
                                                                                                        .stream()
                                                                                                        .map(WithdrawResponse::from)
                                                                                                        .collect(Collectors
                                                                                                                        .toList());

                                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                                        pagedResult.getTotalRecords(),
                                                                                                        page + 1,
                                                                                                        pageSize,
                                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                                        .getTotalRecords()
                                                                                                                        / pageSize));

                                                                                        ApiResponsePagination<List<WithdrawResponse>> response = new ApiResponsePagination<>(
                                                                                                        "success",
                                                                                                        "Withdraws by card number retrieved successfully",
                                                                                                        data, meta);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> response);
                                                                                });
                                                        });
                                }).onFailure()
                                .recoverWithItem(e -> new ApiResponsePagination<>("error",
                                                "Failed to fetch withdraws by card number", Collections.emptyList(),
                                                null));
        }

        @Override
        public Uni<ApiResponse<WithdrawResponse>> findById(Long withdrawId) {
                String cacheKey = String.format("withdraws:id:%d", withdrawId);
                Attributes attrs = Attributes.builder().put("withdrawId", withdrawId).build();

                logger.info("Finding withdraw by id={}", withdrawId);

                return tracingMetrics.traceAndMeasure("findWithdrawById", "find_by_id", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        WithdrawResponse cached = fromJson(cachedJson,
                                                                        new TypeReference<WithdrawResponse>() {
                                                                        });
                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                        "Withdraw retrieved successfully", cached));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return withdrawQueryRepository.findById(withdrawId)
                                                                .chain(w -> {
                                                                        if (w == null) {
                                                                                logger.warn("Withdraw not found with id {}",
                                                                                                withdrawId);
                                                                                throw new ResourceNotFoundException(
                                                                                                "Withdraw not found with id "
                                                                                                                + withdrawId);
                                                                        }

                                                                        WithdrawResponse response = WithdrawResponse
                                                                                        .from(w);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> ApiResponse.success(
                                                                                                        "Withdraw retrieved successfully",
                                                                                                        response));
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", "Withdraw not found", null));
        }

        @Override
        public Uni<ApiResponse<List<WithdrawResponse>>> findByCard(String cardNumber) {
                String cacheKey = String.format("withdraws:list:card:%s", cardNumber);
                Attributes attrs = Attributes.builder().put("cardNumber", cardNumber).build();

                logger.info("Finding withdraws list by card number={}", cardNumber);

                return tracingMetrics.traceAndMeasure("findWithdrawsByCardNumberList", "find_by_card", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        List<WithdrawResponse> cached = fromJson(cachedJson,
                                                                        new TypeReference<List<WithdrawResponse>>() {
                                                                        });
                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                        "Withdraws by card number retrieved successfully",
                                                                        cached));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return withdrawQueryRepository.findByCardNumber(cardNumber)
                                                                .chain(withdraws -> {
                                                                        List<WithdrawResponse> data = withdraws.stream()
                                                                                        .map(WithdrawResponse::from)
                                                                                        .collect(Collectors.toList());

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(data),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> ApiResponse.success(
                                                                                                        "Withdraws by card number retrieved successfully",
                                                                                                        data));
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error",
                                "Failed to fetch withdraws by card number", Collections.emptyList()));
        }

        @Override
        public Uni<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> findByActive(FindAllWithdraws req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("withdraws:active:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching active withdraws | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findActiveWithdraws", "find_active", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<WithdrawResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<WithdrawResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return withdrawQueryRepository
                                                                .findActiveWithdraws(req)
                                                                .chain(pagedResult -> {
                                                                        List<WithdrawResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(WithdrawResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<WithdrawResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Active withdraws retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error",
                                "Failed to fetch active withdraws", Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<WithdrawResponseDeleteAt>>> findByTrashed(FindAllWithdraws req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("withdraws:trashed:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching trashed withdraws | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findTrashedWithdraws", "find_trashed", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<WithdrawResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<WithdrawResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return withdrawQueryRepository
                                                                .findTrashedWithdraws(req)
                                                                .chain(pagedResult -> {
                                                                        List<WithdrawResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(WithdrawResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<WithdrawResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Trashed withdraws retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error",
                                "Failed to fetch trashed withdraws", Collections.emptyList(), null));
        }
}