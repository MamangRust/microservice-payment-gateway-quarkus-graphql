package com.sanedge.transfer.service.impl;

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
import com.sanedge.transfer.domain.requests.FindAllTransfers;
import com.sanedge.transfer.domain.response.TransferResponse;
import com.sanedge.transfer.domain.response.TransferResponseDeleteAt;
import com.sanedge.transfer.repository.TransferQueryRepository;
import com.sanedge.transfer.service.TransferQueryService;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TransferQueryServiceImpl implements TransferQueryService {
        private static final Logger logger = LoggerFactory.getLogger(TransferQueryServiceImpl.class);

        private final TransferQueryRepository transferQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long QUERY_CACHE_TTL_SECONDS = 300;

        @Inject
        public TransferQueryServiceImpl(TransferQueryRepository transferQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.transferQueryRepository = transferQueryRepository;
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
        public Uni<ApiResponsePagination<List<TransferResponse>>> findAll(FindAllTransfers req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transfers:all:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching all transfers | Page: {}, Size: {}, Search: {}", page + 1, pageSize,
                                search.isEmpty() ? "None" : search);

                return tracingMetrics.traceAndMeasure("findAllTransfers", "find_all", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransferResponse>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransferResponse>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transferQueryRepository.findTransfers(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransferResponse> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransferResponse::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransferResponse>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Transfers retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error", "Failed to fetch transfers",
                                Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponse<TransferResponse>> findById(Long transferId) {
                String cacheKey = String.format("transfers:id:%d", transferId);
                Attributes attrs = Attributes.builder().put("transferId", transferId).build();

                logger.info("Finding transfer by id={}", transferId);

                return tracingMetrics.traceAndMeasure("findTransferById", "find_by_id", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        TransferResponse cached = fromJson(cachedJson,
                                                                        new TypeReference<TransferResponse>() {
                                                                        });
                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                        "Transfer retrieved successfully", cached));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transferQueryRepository.findTransferById(transferId)
                                                                .chain(t -> {
                                                                        if (t == null) {
                                                                                logger.warn("Transfer not found with id {}",
                                                                                                transferId);
                                                                                throw new ResourceNotFoundException(
                                                                                                "Transfer not found with id "
                                                                                                                + transferId);
                                                                        }

                                                                        TransferResponse response = TransferResponse
                                                                                        .from(t);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> ApiResponse.success(
                                                                                                        "Transfer retrieved successfully",
                                                                                                        response));
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error", e.getMessage(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<TransferResponseDeleteAt>>> findByActive(FindAllTransfers req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transfers:active:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching active transfers | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findActiveTransfers", "find_active", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransferResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransferResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transferQueryRepository
                                                                .findActiveTransfers(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransferResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransferResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransferResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Active transfers retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error",
                                "Failed to fetch active transfers", Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponsePagination<List<TransferResponseDeleteAt>>> findByTrashed(FindAllTransfers req) {
                int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
                int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
                String search = req.getSearch() != null ? req.getSearch() : "";

                String cacheKey = String.format("transfers:trashed:%d:%d:%s", page, pageSize, search);
                Attributes attrs = Attributes.builder()
                                .put("page", page)
                                .put("size", pageSize)
                                .build();

                logger.info("Searching trashed transfers | Page: {}, Size: {}", page + 1, pageSize);

                return tracingMetrics.traceAndMeasure("findTrashedTransfers", "find_trashed", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<TransferResponseDeleteAt>> cached = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<TransferResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(cached);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transferQueryRepository
                                                                .findTrashedTransfers(req)
                                                                .chain(pagedResult -> {
                                                                        List<TransferResponseDeleteAt> data = pagedResult
                                                                                        .getData().stream()
                                                                                        .map(TransferResponseDeleteAt::from)
                                                                                        .collect(Collectors.toList());

                                                                        PaginationMeta meta = new PaginationMeta(
                                                                                        pagedResult.getTotalRecords(),
                                                                                        page + 1,
                                                                                        pageSize,
                                                                                        (int) Math.ceil((double) pagedResult
                                                                                                        .getTotalRecords()
                                                                                                        / pageSize));

                                                                        ApiResponsePagination<List<TransferResponseDeleteAt>> response = new ApiResponsePagination<>(
                                                                                        "success",
                                                                                        "Trashed transfers retrieved successfully",
                                                                                        data, meta);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> response);
                                                                });
                                        });
                }).onFailure().recoverWithItem(e -> new ApiResponsePagination<>("error",
                                "Failed to fetch trashed transfers", Collections.emptyList(), null));
        }

        @Override
        public Uni<ApiResponse<List<TransferResponse>>> findByTransferFrom(String transferFrom) {
                String cacheKey = String.format("transfers:from:%s", transferFrom);
                Attributes attrs = Attributes.builder().put("transferFrom", transferFrom).build();

                logger.info("Finding transfers by transferFrom={}", transferFrom);

                return tracingMetrics
                                .traceAndMeasure("findTransfersByTransferFrom", "find_by_transfer_from", attrs, () -> {
                                        return redisService.getReactive(cacheKey)
                                                        .chain(cachedJson -> {
                                                                if (cachedJson != null) {
                                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                                        List<TransferResponse> cached = fromJson(
                                                                                        cachedJson,
                                                                                        new TypeReference<List<TransferResponse>>() {
                                                                                        });
                                                                        return Uni.createFrom().item(ApiResponse
                                                                                        .success("Transfers by sender retrieved successfully",
                                                                                                        cached));
                                                                }

                                                                logger.info("Cache MISS for key: {}. Fetching from DB.",
                                                                                cacheKey);
                                                                return transferQueryRepository
                                                                                .findTransfersBySourceCard(transferFrom)
                                                                                .chain(transfers -> {
                                                                                        List<TransferResponse> data = transfers
                                                                                                        .stream()
                                                                                                        .map(TransferResponse::from)
                                                                                                        .collect(Collectors
                                                                                                                        .toList());

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(data),
                                                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> ApiResponse
                                                                                                                        .success("Transfers by sender retrieved successfully",
                                                                                                                                        data));
                                                                                });
                                                        });
                                }).onFailure().recoverWithItem(e -> new ApiResponse<>("error",
                                                "Failed to fetch transfers", Collections.emptyList()));
        }

        @Override
        public Uni<ApiResponse<List<TransferResponse>>> findByTransferTo(String transferTo) {
                String cacheKey = String.format("transfers:to:%s", transferTo);
                Attributes attrs = Attributes.builder().put("transferTo", transferTo).build();

                logger.info("Finding transfers by transferTo={}", transferTo);

                return tracingMetrics.traceAndMeasure("findTransfersByTransferTo", "find_by_transfer_to", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        List<TransferResponse> cached = fromJson(cachedJson,
                                                                        new TypeReference<List<TransferResponse>>() {
                                                                        });
                                                        return Uni.createFrom().item(ApiResponse.success(
                                                                        "Transfers by receiver retrieved successfully",
                                                                        cached));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return transferQueryRepository
                                                                .findTransfersByDestinationCard(transferTo)
                                                                .chain(transfers -> {
                                                                        List<TransferResponse> data = transfers.stream()
                                                                                        .map(TransferResponse::from)
                                                                                        .collect(Collectors.toList());

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(data),
                                                                                        QUERY_CACHE_TTL_SECONDS)
                                                                                        .map(v -> ApiResponse.success(
                                                                                                        "Transfers by receiver retrieved successfully",
                                                                                                        data));
                                                                });
                                        });
                }).onFailure().recoverWithItem(
                                e -> new ApiResponse<>("error", "Failed to fetch transfers", Collections.emptyList()));
        }
}