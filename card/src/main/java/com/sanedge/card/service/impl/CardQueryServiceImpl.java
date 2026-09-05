package com.sanedge.card.service.impl;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.card.domain.requests.FindAllCards;
import com.sanedge.card.domain.response.CardResponse;
import com.sanedge.card.domain.response.CardResponseDeleteAt;
import com.sanedge.card.entity.Card;
import com.sanedge.card.repository.CardQueryRepository;
import com.sanedge.card.service.CardQueryService;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.domain.response.PaginationMeta;
import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class CardQueryServiceImpl implements CardQueryService {
        private static final Logger logger = LoggerFactory.getLogger(CardQueryServiceImpl.class);

        private final CardQueryRepository cardQueryRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public CardQueryServiceImpl(CardQueryRepository cardQueryRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.cardQueryRepository = cardQueryRepository;
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
        public Uni<ApiResponsePagination<List<CardResponse>>> findAll(FindAllCards req) {
                String cacheKey = String.format("cards:all:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CardResponse>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CardResponse>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        Attributes attrs = Attributes.builder()
                                                        .put("card.page", req.getPage())
                                                        .put("card.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findAllCards", "find_all_cards", attrs,
                                                        () -> {
                                                                return cardQueryRepository.findCards(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<CardResponse>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req,
                                                                                                        "Cards retrieved successfully",
                                                                                                        CardResponse::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} cards",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<CardResponseDeleteAt>>> findByActive(FindAllCards req) {
                String cacheKey = String.format("cards:active:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CardResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CardResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        Attributes attrs = Attributes.builder()
                                                        .put("card.page", req.getPage())
                                                        .put("card.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findActiveCards", "find_active_cards",
                                                        attrs, () -> {
                                                                return cardQueryRepository
                                                                                .findActiveCards(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<CardResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req,
                                                                                                        "Active cards retrieved successfully",
                                                                                                        CardResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} active cards",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponsePagination<List<CardResponseDeleteAt>>> findByTrashed(FindAllCards req) {
                String cacheKey = String.format("cards:trashed:%d:%d:%s", req.getPage(), req.getPageSize(),
                                req.getSearch());

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                ApiResponsePagination<List<CardResponseDeleteAt>> response = fromJson(
                                                                cachedJson,
                                                                new TypeReference<ApiResponsePagination<List<CardResponseDeleteAt>>>() {
                                                                });
                                                return Uni.createFrom().item(response);
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);

                                        Attributes attrs = Attributes.builder()
                                                        .put("card.page", req.getPage())
                                                        .put("card.size", req.getPageSize())
                                                        .build();

                                        return tracingMetrics.traceAndMeasure("findTrashedCards", "find_trashed_cards",
                                                        attrs, () -> {
                                                                return cardQueryRepository
                                                                                .findTrashedCards(req)
                                                                                .chain(pagedResult -> {
                                                                                        ApiResponsePagination<List<CardResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                                        pagedResult,
                                                                                                        req,
                                                                                                        "Trashed cards retrieved successfully",
                                                                                                        CardResponseDeleteAt::from);

                                                                                        return redisService
                                                                                                        .setWithExpirationReactive(
                                                                                                                        cacheKey,
                                                                                                                        toJson(response),
                                                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached response for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully retrieved {} trashed cards",
                                                                                                                                pagedResult.getTotalRecords());
                                                                                                                return response;
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponse<CardResponse>> findById(Long cardId) {
                String cacheKey = "card:id:" + cardId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                CardResponse cachedCard = fromJson(cachedJson, CardResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Card retrieved successfully", cachedCard));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder().put("card.id", cardId).build();

                                        return tracingMetrics.<ApiResponse<CardResponse>>traceAndMeasure("findCardById",
                                                        "find_card_by_id", attrs,
                                                        () -> {
                                                                return cardQueryRepository.findCardById(cardId)
                                                                                .chain(opt -> {
                                                                                        if (opt.isEmpty()) {
                                                                                                logger.warn("Card not found with id: {}",
                                                                                                                cardId);
                                                                                                throw new NotFoundException(
                                                                                                                "Card not found with id: "
                                                                                                                                + cardId);
                                                                                        }

                                                                                        Card card = opt.get();
                                                                                        CardResponse cardResponse = CardResponse
                                                                                                        .from(card);

                                                                                        return redisService.setReactive(
                                                                                                        cacheKey,
                                                                                                        toJson(cardResponse))
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached card for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully found card with id: {} and number: {}",
                                                                                                                                cardId,
                                                                                                                                card.getCardNumber());
                                                                                                                return ApiResponse
                                                                                                                                .success("Card retrieved successfully",
                                                                                                                                                cardResponse);
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponse<CardResponse>> findByUserId(Long userId) {
                String cacheKey = "card:user:" + userId;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                CardResponse cachedCard = fromJson(cachedJson, CardResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Card retrieved successfully", cachedCard));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder().put("user.id", userId).build();

                                        return tracingMetrics.<ApiResponse<CardResponse>>traceAndMeasure(
                                                        "findCardByUserId",
                                                        "find_card_by_user_id", attrs, () -> {
                                                                return cardQueryRepository.findCardByUserId(userId)
                                                                                .chain(opt -> {
                                                                                        if (opt.isEmpty()) {
                                                                                                logger.warn("Card not found for user id: {}",
                                                                                                                userId);
                                                                                                throw new NotFoundException(
                                                                                                                "Card not found for user id: "
                                                                                                                                + userId);
                                                                                        }

                                                                                        Card card = opt.get();
                                                                                        CardResponse cardResponse = CardResponse
                                                                                                        .from(card);

                                                                                        return redisService.setReactive(
                                                                                                        cacheKey,
                                                                                                        toJson(cardResponse))
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached card for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully found card for user id: {} and number: {}",
                                                                                                                                userId,
                                                                                                                                card.getCardNumber());
                                                                                                                return ApiResponse
                                                                                                                                .success("Card retrieved successfully",
                                                                                                                                                cardResponse);
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        @Override
        public Uni<ApiResponse<CardResponse>> findByCardNumber(String cardNumber) {
                String cacheKey = "card:number:" + cardNumber;

                return redisService.getReactive(cacheKey)
                                .chain(cachedJson -> {
                                        if (cachedJson != null) {
                                                logger.info("Cache HIT for key: {}", cacheKey);
                                                CardResponse cachedCard = fromJson(cachedJson, CardResponse.class);
                                                return Uni.createFrom().item(ApiResponse
                                                                .success("Card retrieved successfully", cachedCard));
                                        }

                                        logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                        Attributes attrs = Attributes.builder().put("card.number", cardNumber).build();

                                        return tracingMetrics.<ApiResponse<CardResponse>>traceAndMeasure(
                                                        "findCardByCardNumber",
                                                        "find_card_by_card_number", attrs, () -> {
                                                                return cardQueryRepository
                                                                                .findCardByCardNumber(cardNumber)
                                                                                .chain(opt -> {
                                                                                        if (opt.isEmpty()) {
                                                                                                logger.warn("Card not found with card number: {}",
                                                                                                                cardNumber);
                                                                                                throw new NotFoundException(
                                                                                                                "Card not found with card number: "
                                                                                                                                + cardNumber);
                                                                                        }

                                                                                        Card card = opt.get();
                                                                                        CardResponse cardResponse = CardResponse
                                                                                                        .from(card);

                                                                                        return redisService.setReactive(
                                                                                                        cacheKey,
                                                                                                        toJson(cardResponse))
                                                                                                        .map(v -> {
                                                                                                                logger.info("Cached card for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully found card with card number: {}",
                                                                                                                                cardNumber);
                                                                                                                return ApiResponse
                                                                                                                                .success("Card retrieved successfully",
                                                                                                                                                cardResponse);
                                                                                                        });
                                                                                });
                                                        });
                                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllCards request,
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