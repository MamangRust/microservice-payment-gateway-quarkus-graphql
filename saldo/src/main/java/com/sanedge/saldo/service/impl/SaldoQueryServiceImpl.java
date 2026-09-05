package com.sanedge.saldo.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.saldo.domain.requests.FindAllSaldos;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.saldo.domain.response.SaldoResponse;
import com.sanedge.saldo.domain.response.SaldoResponseDeleteAt;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.saldo.repository.SaldoQueryRepository;
import com.sanedge.saldo.service.SaldoQueryService;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SaldoQueryServiceImpl implements SaldoQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SaldoQueryServiceImpl.class);

    private final SaldoQueryRepository saldoQueryRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final TracingMetrics tracingMetrics;

    private static final long LIST_CACHE_TTL_SECONDS = 300;

    @Inject
    public SaldoQueryServiceImpl(SaldoQueryRepository saldoQueryRepository,
            RedisService redisService,
            ObjectMapper objectMapper,
            TracingMetrics tracingMetrics) {
        this.saldoQueryRepository = saldoQueryRepository;
        this.redisService = redisService;
        this.objectMapper = objectMapper;
        this.tracingMetrics = tracingMetrics;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object to JSON", e);
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON using TypeReference", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize JSON to class", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public Uni<ApiResponsePagination<List<SaldoResponse>>> findAll(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("saldos:all:%d:%d:%s", page, pageSize, keyword);

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<SaldoResponse>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<SaldoResponse>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from database.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findAllSaldos", "find_all_saldos",
                            () -> saldoQueryRepository.findSaldos(req)
                                    .chain(pagedResult -> {
                                        ApiResponsePagination<List<SaldoResponse>> response = buildPaginatedResponse(
                                                pagedResult, req, "Get all saldos success", SaldoResponse::from);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        LIST_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Successfully cached response for key: {}", cacheKey);
                                                    logger.info("Successfully retrieved {} saldos",
                                                            pagedResult.getTotalRecords());
                                                    return response;
                                                });
                                    }));
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<SaldoResponseDeleteAt>>> findActive(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("saldos:active:%d:%d:%s", page, pageSize, keyword);

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<SaldoResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<SaldoResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from database.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findActiveSaldos", "find_active_saldos",
                            () -> saldoQueryRepository.findActiveSaldos(req)
                                    .chain(pagedResult -> {
                                        ApiResponsePagination<List<SaldoResponseDeleteAt>> response = buildPaginatedResponse(
                                                pagedResult, req, "Get active saldos success",
                                                SaldoResponseDeleteAt::from);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        LIST_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Successfully cached response for key: {}", cacheKey);
                                                    logger.info("Successfully retrieved {} active saldos",
                                                            pagedResult.getTotalRecords());
                                                    return response;
                                                });
                                    }));
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<SaldoResponseDeleteAt>>> findTrashed(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("saldos:trashed:%d:%d:%s", page, pageSize, keyword);

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<SaldoResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<SaldoResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from database.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findTrashedSaldos", "find_trashed_saldos",
                            () -> saldoQueryRepository.findTrashedSaldos(req)
                                    .chain(pagedResult -> {
                                        ApiResponsePagination<List<SaldoResponseDeleteAt>> response = buildPaginatedResponse(
                                                pagedResult, req, "Get trashed saldos success",
                                                SaldoResponseDeleteAt::from);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        LIST_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Successfully cached response for key: {}", cacheKey);
                                                    logger.info("Successfully retrieved {} trashed saldos",
                                                            pagedResult.getTotalRecords());
                                                    return response;
                                                });
                                    }));
                });
    }

    @Override
    public Uni<ApiResponse<SaldoResponse>> findByCard(String cardNumber) {
        String cacheKey = "saldo:card:" + cardNumber;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        SaldoResponse cachedSaldo = fromJson(cachedJson, SaldoResponse.class);
                        return Uni.createFrom().item(ApiResponse.success("Get saldo success", cachedSaldo));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from database.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findSaldoByCard", "find_saldo_by_card",
                            () -> saldoQueryRepository.findByCardNumber(cardNumber)
                                    .chain(saldo -> {
                                        if (saldo == null) {
                                            logger.warn("Saldo not found for card: {}", cardNumber);
                                            throw new ResourceNotFoundException("Saldo not found");
                                        }

                                        SaldoResponse response = SaldoResponse.from(saldo);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        LIST_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Successfully cached response for key: {}", cacheKey);
                                                    logger.info("Successfully found saldo for card: {}", cardNumber);
                                                    return ApiResponse.success("Get saldo success", response);
                                                });
                                    }))
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to find saldo by card: {}", cardNumber, e);
                                return new ApiResponse<>("error", e.getMessage(), null);
                            });
                });
    }

    @Override
    public Uni<ApiResponse<SaldoResponse>> findById(Long id) {
        String cacheKey = "saldo:id:" + id;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        SaldoResponse cachedSaldo = fromJson(cachedJson, SaldoResponse.class);
                        return Uni.createFrom().item(ApiResponse.success("Get saldo success", cachedSaldo));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from database.", cacheKey);

                    return tracingMetrics.traceAndMeasure("findSaldoById", "find_saldo_by_id",
                            () -> saldoQueryRepository.findById(id)
                                    .chain(saldo -> {
                                        if (saldo == null) {
                                            logger.warn("Saldo not found for id: {}", id);
                                            throw new ResourceNotFoundException("Saldo not found");
                                        }

                                        SaldoResponse response = SaldoResponse.from(saldo);

                                        return redisService
                                                .setWithExpirationReactive(cacheKey, toJson(response),
                                                        LIST_CACHE_TTL_SECONDS)
                                                .map(v -> {
                                                    logger.info("Successfully cached response for key: {}", cacheKey);
                                                    logger.info("Successfully found saldo for id: {}", id);
                                                    return ApiResponse.success("Get saldo success", response);
                                                });
                                    }))
                            .onFailure().recoverWithItem(e -> {
                                logger.error("Failed to find saldo by id: {}", id, e);
                                return new ApiResponse<>("error", e.getMessage(), null);
                            });
                });
    }

    private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
            PagedResult<T> pagedResult,
            FindAllSaldos request,
            String successMessage,
            Function<T, R> mapper) {

        List<R> data = pagedResult.getData().stream()
                .map(mapper)
                .collect(Collectors.toList());

        int totalRecords = pagedResult.getTotalRecords();
        int size = request.getPageSize() > 0 ? request.getPageSize() : 1;
        int totalPages = (int) Math.ceil((double) totalRecords / size);

        PaginationMeta pagination = new PaginationMeta(request.getPage(), size, totalPages, totalRecords);

        return new ApiResponsePagination<>("success", successMessage, data, pagination);
    }
}