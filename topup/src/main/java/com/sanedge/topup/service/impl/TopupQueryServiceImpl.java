package com.sanedge.topup.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.topup.domain.requests.FindAllTopups;
import com.sanedge.topup.domain.requests.FindAllTopupsByCardNumber;
import com.sanedge.topup.domain.response.TopupResponse;
import com.sanedge.topup.domain.response.TopupResponseDeleteAt;
import com.sanedge.topup.repository.TopupQueryRepository;
import com.sanedge.topup.service.TopupQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TopupQueryServiceImpl implements TopupQueryService {
        private static final Logger logger = LoggerFactory.getLogger(TopupQueryServiceImpl.class);

        private final TopupQueryRepository topupQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public TopupQueryServiceImpl(TopupQueryRepository topupQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.topupQueryRepository = topupQueryRepository;
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

        private <T> T fromJson(String json, Class<T> clazz) {
                try {
                        return objectMapper.readValue(json, clazz);
                } catch (JsonProcessingException e) {
                        logger.error("Error deserializing JSON to class", e);
                        throw new RuntimeException("Failed to deserialize JSON", e);
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
        public Uni<ApiResponsePagination<List<TopupResponse>>> findAll(FindAllTopups req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("topups:all:%d:%d:%s", page, pageSize, search);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TopupResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TopupResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("topup.page", req.getPage())
                                                        .put("topup.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findAllTopups", "find_all_topups", attrs,
                                                        () -> {
                                                                return topupQueryRepository
                                                                                .findTopups(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<TopupResponse>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Found " + pagedResult
                                                                                                                        .getData()
                                                                                                                        .size()
                                                                                                                        + " topups",
                                                                                                        TopupResponse::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TopupResponse>>> findAllByCardNumber(FindAllTopupsByCardNumber req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String cardNumber = req.getCardNumber();
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("topups:card-num:%s:%d:%d:%s", cardNumber, page, pageSize, search);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TopupResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TopupResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("cardNumber", cardNumber)
                                                        .put("topup.page", req.getPage())
                                                        .put("topup.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findAllTopupsByCardNumber",
                                                        "find_all_topups_by_card_number", attrs, () -> {
                                                                return topupQueryRepository
                                                                                .findTopupByCard(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<TopupResponse>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Found " + pagedResult
                                                                                                                        .getData()
                                                                                                                        .size()
                                                                                                                        + " topups for card="
                                                                                                                        + cardNumber,
                                                                                                        TopupResponse::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TopupResponseDeleteAt>>> findActive(FindAllTopups req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("topups:active:%d:%d:%s", page, pageSize, search);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TopupResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TopupResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("topup.page", req.getPage())
                                                        .put("topup.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findActiveTopups", "find_active_topups",
                                                        attrs, () -> {
                                                                return topupQueryRepository
                                                                                .findActiveTopups(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<TopupResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Found " + pagedResult
                                                                                                                        .getData()
                                                                                                                        .size()
                                                                                                                        + " active topups",
                                                                                                        TopupResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<TopupResponseDeleteAt>>> findTrashed(FindAllTopups req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("topups:trashed:%d:%d:%s", page, pageSize, search);

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<TopupResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<TopupResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder()
                                                        .put("topup.page", req.getPage())
                                                        .put("topup.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findTrashedTopups",
                                                        "find_trashed_topups", attrs, () -> {
                                                                return topupQueryRepository
                                                                                .findTrashedTopups(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<TopupResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req.getPage(),
                                                                                                        req.getPageSize(),
                                                                                                        "Found " + pagedResult
                                                                                                                        .getData()
                                                                                                                        .size()
                                                                                                                        + " trashed topups",
                                                                                                        TopupResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponse<List<TopupResponse>>> findByCard(String cardNumber) {
                String cacheKey = "topups:card:" + cardNumber;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                List<TopupResponse> cachedTopups = fromJson(cachedJson,
                                                                new TypeReference<List<TopupResponse>>() {
                                                                });
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Found " + cachedTopups.size() + " topups for card="
                                                                                + cardNumber,
                                                                cachedTopups));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder().put("cardNumber", cardNumber).build();

                                        return tracingMetrics.traceAndMeasure("findTopupsByCard", "find_topups_by_card",
                                                        attrs, () -> {
                                                                return topupQueryRepository.findByCardNumber(cardNumber)
                                                                                .chain(topups -> {
                                                                                        List<TopupResponse> responseList = topups
                                                                                                        .stream()
                                                                                                        .map(TopupResponse::from)
                                                                                                        .collect(Collectors
                                                                                                                        .toList());

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(responseList),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return ApiResponse
                                                                                                                                .success(
                                                                                                                                                "Found " + responseList
                                                                                                                                                                .size()
                                                                                                                                                                + " topups for card="
                                                                                                                                                                + cardNumber,
                                                                                                                                                responseList);
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponse<TopupResponse>> findById(Long topupId) {
                String cacheKey = "topup:id:" + topupId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                TopupResponse cachedTopup = fromJson(cachedJson, TopupResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Found topup id=" + topupId, cachedTopup));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder().put("topupId", topupId).build();

                                        return tracingMetrics.traceAndMeasure("findTopupById", "find_topup_by_id",
                                                        attrs, () -> {
                                                                return topupQueryRepository.findTopupById(topupId)
                                                                                .chain(topup -> {
                                                                                        if (topup == null) {
                                                                                                logger.warn("Topup not found id={}",
                                                                                                                topupId);
                                                                                                throw new ResourceNotFoundException(
                                                                                                                "Topup not found");
                                                                                        }

                                                                                        TopupResponse response = TopupResponse
                                                                                                        .from(topup);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                return ApiResponse
                                                                                                                                .success("Found topup id="
                                                                                                                                                + topupId,
                                                                                                                                                response);
                                                                                                        });
                                                                                });
                                                        }).onFailure().recoverWithItem(e -> new ApiResponse<>("error",
                                                                        e.getMessage(), null));
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        int reqPage,
                        int reqSize,
                        String successMessage,
                        Function<T, R> mapper) {

                List<R> data = pagedResult.getData().stream()
                                .map(mapper)
                                .collect(Collectors.toList());

                int totalRecords = pagedResult.getTotalRecords();
                int size = reqSize > 0 ? reqSize : 1;
                int totalPages = (int) Math.ceil((double) totalRecords / size);

                PaginationMeta pagination = new PaginationMeta(reqPage, size, totalPages, totalRecords);

                return new ApiResponsePagination<>("success", successMessage, data, pagination);
        }
}