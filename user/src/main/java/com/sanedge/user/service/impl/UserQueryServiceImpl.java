package com.sanedge.user.service.impl;

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
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;
import com.sanedge.user.repository.UserRepository;
import com.sanedge.user.service.UserQueryService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class UserQueryServiceImpl implements UserQueryService {
        private static final Logger logger = LoggerFactory.getLogger(UserQueryServiceImpl.class);

        private final UserRepository userRepository;
        private final RedisService redisService;
        private final ObjectMapper objectMapper;
        private final TracingMetrics tracingMetrics;

        private static final long LIST_CACHE_TTL_SECONDS = 300;

        @Inject
        public UserQueryServiceImpl(UserRepository userRepository,
                        RedisService redisService,
                        ObjectMapper objectMapper,
                        TracingMetrics tracingMetrics) {
                this.userRepository = userRepository;
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
        @WithSession
        public Uni<ApiResponsePagination<List<UserResponse>>> findAllPaginated(FindAllUsers request) {
                String cacheKey = String.format("users:all:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());
                Attributes attrs = Attributes.builder()
                                .put("user.page", request.getPage())
                                .put("user.size", request.getPageSize())
                                .build();

                return tracingMetrics.traceAndMeasure("findAllUsers", "find_all_users", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<UserResponse>> response = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<UserResponse>>>() {
                                                                        });
                                                        return Uni.createFrom().item(response);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return userRepository.findUsers(request)
                                                                .chain(pagedResult -> {
                                                                        ApiResponsePagination<List<UserResponse>> response = buildPaginatedResponse(
                                                                                        pagedResult, request,
                                                                                        "Users retrieved successfully",
                                                                                        UserResponse::from);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                        .map(v -> {
                                                                                                logger.info("Cached response for key: {}",
                                                                                                                cacheKey);
                                                                                                logger.info("Successfully retrieved {} users",
                                                                                                                pagedResult.getTotalRecords());
                                                                                                return response;
                                                                                        });
                                                                });
                                        });
                });
        }

        @Override
        @WithSession
        public Uni<ApiResponsePagination<List<UserResponseDeleteAt>>> findActivePaginated(FindAllUsers request) {
                String cacheKey = String.format("users:active:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());
                Attributes attrs = Attributes.builder()
                                .put("user.page", request.getPage())
                                .put("user.size", request.getPageSize())
                                .build();

                return tracingMetrics.traceAndMeasure("findActiveUsers", "find_active_users", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<UserResponseDeleteAt>> response = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<UserResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(response);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return userRepository.findActiveUsers(request)
                                                                .chain(pagedResult -> {
                                                                        ApiResponsePagination<List<UserResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                        pagedResult, request,
                                                                                        "Active users retrieved successfully",
                                                                                        UserResponseDeleteAt::from);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                        .map(v -> {
                                                                                                logger.info("Cached response for key: {}",
                                                                                                                cacheKey);
                                                                                                logger.info("Successfully retrieved {} active users",
                                                                                                                pagedResult.getTotalRecords());
                                                                                                return response;
                                                                                        });
                                                                });
                                        });
                });
        }

        @Override
        @WithSession
        public Uni<ApiResponsePagination<List<UserResponseDeleteAt>>> findTrashedPaginated(FindAllUsers request) {
                String cacheKey = String.format("users:trashed:%d:%d:%s", request.getPage(), request.getPageSize(),
                                request.getSearch());
                Attributes attrs = Attributes.builder()
                                .put("user.page", request.getPage())
                                .put("user.size", request.getPageSize())
                                .build();

                return tracingMetrics.traceAndMeasure("findTrashedUsers", "find_trashed_users", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        ApiResponsePagination<List<UserResponseDeleteAt>> response = fromJson(
                                                                        cachedJson,
                                                                        new TypeReference<ApiResponsePagination<List<UserResponseDeleteAt>>>() {
                                                                        });
                                                        return Uni.createFrom().item(response);
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return userRepository.findTrashedUsers(request)
                                                                .chain(pagedResult -> {
                                                                        ApiResponsePagination<List<UserResponseDeleteAt>> response = buildPaginatedResponse(
                                                                                        pagedResult, request,
                                                                                        "Trashed users retrieved successfully",
                                                                                        UserResponseDeleteAt::from);

                                                                        return redisService.setWithExpirationReactive(
                                                                                        cacheKey, toJson(response),
                                                                                        LIST_CACHE_TTL_SECONDS)
                                                                                        .map(v -> {
                                                                                                logger.info("Cached response for key: {}",
                                                                                                                cacheKey);
                                                                                                logger.info("Successfully retrieved {} trashed users",
                                                                                                                pagedResult.getTotalRecords());
                                                                                                return response;
                                                                                        });
                                                                });
                                        });
                });
        }

        @Override
        @WithSession
        public Uni<ApiResponse<UserResponse>> findById(Long id) {
                String cacheKey = "user:" + id;
                Attributes attrs = Attributes.builder().put("user.id", id).build();

                return tracingMetrics.traceAndMeasure("findUserById", "find_user_by_id", attrs, () -> {
                        return redisService.getReactive(cacheKey)
                                        .chain(cachedJson -> {
                                                if (cachedJson != null) {
                                                        logger.info("Cache HIT for key: {}", cacheKey);
                                                        UserResponse cachedUser = fromJson(cachedJson,
                                                                        UserResponse.class);
                                                        return Uni.createFrom().item(
                                                                        ApiResponse.success("User found", cachedUser));
                                                }

                                                logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                                                return userRepository.findById(id)
                                                                .chain(user -> {
                                                                        if (user == null) {
                                                                                logger.warn("User not found with id: {}",
                                                                                                id);
                                                                                throw new NotFoundException(
                                                                                                "User not found with id: "
                                                                                                                + id);
                                                                        }

                                                                        UserResponse userResponse = UserResponse
                                                                                        .from(user);

                                                                        return redisService
                                                                                        .setReactive(cacheKey, toJson(
                                                                                                        userResponse))
                                                                                        .map(v -> {
                                                                                                logger.info("Cached user for key: {}",
                                                                                                                cacheKey);
                                                                                                logger.info("Successfully found user with id: {} and username: {}",
                                                                                                                id,
                                                                                                                user.getUsername());
                                                                                                return ApiResponse
                                                                                                                .success("User found",
                                                                                                                                userResponse);
                                                                                        });
                                                                });
                                        });
                });
        }

        private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
                        PagedResult<T> pagedResult,
                        FindAllUsers request,
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