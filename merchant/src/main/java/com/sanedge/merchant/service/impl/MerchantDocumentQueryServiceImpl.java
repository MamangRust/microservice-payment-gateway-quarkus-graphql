package com.sanedge.merchant.service.impl;

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
import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.merchant.domain.response.MerchantDocumentResponse;
import com.sanedge.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import com.sanedge.merchant.repository.MerchantDocumentQueryRepository;
import com.sanedge.merchant.service.MerchantDocumentQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class MerchantDocumentQueryServiceImpl implements MerchantDocumentQueryService {
    private static final Logger logger = LoggerFactory.getLogger(MerchantDocumentQueryServiceImpl.class);

    private final MerchantDocumentQueryRepository merchantDocumentQueryRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;
    private final TracingMetrics tracingMetrics;

    private static final long LIST_CACHE_TTL_SECONDS = 300;

    @Inject
    public MerchantDocumentQueryServiceImpl(MerchantDocumentQueryRepository merchantDocumentQueryRepository,
            RedisService redisService,
            ObjectMapper objectMapper,
            TracingMetrics tracingMetrics) {
        this.merchantDocumentQueryRepository = merchantDocumentQueryRepository;
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
            logger.error("Error deserializing JSON to object", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing JSON to object with TypeReference", e);
            throw new RuntimeException("Failed to deserialize JSON", e);
        }
    }

    @Override
    public Uni<ApiResponsePagination<List<MerchantDocumentResponse>>> findAll(FindAllMerchantDocuments req) {
        String cacheKey = String.format("merchant_docs:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                req.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<MerchantDocumentResponse>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<MerchantDocumentResponse>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("doc.page", req.getPage())
                            .put("doc.size", req.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findAllMerchantDocuments", "find_all", attrs, () -> {
                        return merchantDocumentQueryRepository.findDocuments(req)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<MerchantDocumentResponse>> response = buildPaginatedResponse(
                                            pagedResult, req, "Merchant documents retrieved successfully",
                                            MerchantDocumentResponse::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findAllActive(
            FindAllMerchantDocuments req) {
        String cacheKey = String.format("merchant_docs:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                req.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("doc.page", req.getPage())
                            .put("doc.size", req.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findActiveMerchantDocuments", "find_active", attrs, () -> {
                        return merchantDocumentQueryRepository.findActiveDocuments(req)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = buildPaginatedResponse(
                                            pagedResult, req, "Active merchant documents retrieved successfully",
                                            MerchantDocumentResponseDeleteAt::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    public Uni<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>> findAllTrashed(
            FindAllMerchantDocuments req) {
        String cacheKey = String.format("merchant_docs:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                req.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("doc.page", req.getPage())
                            .put("doc.size", req.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findTrashedMerchantDocuments", "find_trashed", attrs, () -> {
                        return merchantDocumentQueryRepository.findTrashedDocuments(req)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<MerchantDocumentResponseDeleteAt>> response = buildPaginatedResponse(
                                            pagedResult, req, "Trashed merchant documents retrieved successfully",
                                            MerchantDocumentResponseDeleteAt::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    public Uni<ApiResponse<MerchantDocumentResponse>> findById(Long id) {
        String cacheKey = "merchant_doc:id:" + id;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        MerchantDocumentResponse cachedDoc = fromJson(cachedJson, MerchantDocumentResponse.class);
                        return Uni.createFrom()
                                .item(ApiResponse.success("Merchant document retrieved successfully", cachedDoc));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder().put("doc.id", id).build();

                    return tracingMetrics.traceAndMeasure("findMerchantDocumentById", "find_by_id", attrs, () -> {
                        return merchantDocumentQueryRepository.findDocumentById(id)
                                .chain(doc -> {
                                    if (doc == null) {
                                        logger.warn("Merchant document not found with id: {}", id);
                                        throw new NotFoundException("Merchant document not found with id: " + id);
                                    }

                                    MerchantDocumentResponse response = MerchantDocumentResponse.from(doc);

                                    return redisService.setReactive(cacheKey, toJson(response))
                                            .map(v -> {
                                                logger.info("Cached merchant document for key: {}", cacheKey);
                                                return ApiResponse.success("Merchant document retrieved successfully",
                                                        response);
                                            });
                                });
                    });
                });
    }

    private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
            PagedResult<T> pagedResult,
            FindAllMerchantDocuments request,
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