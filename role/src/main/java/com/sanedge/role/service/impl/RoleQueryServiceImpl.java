package com.sanedge.role.service.impl;

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
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.repository.RoleRepository;
import com.sanedge.role.service.RoleQueryService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class RoleQueryServiceImpl implements RoleQueryService {
    private static final Logger logger = LoggerFactory.getLogger(RoleQueryServiceImpl.class);

    private final RoleRepository roleRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper;

    private static final long LIST_CACHE_TTL_SECONDS = 300;

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
    public Uni<ApiResponsePagination<List<RoleResponse>>> findAllPaginated(FindAllRoles request) {
        String cacheKey = String.format("roles:all:%d:%d:%s", request.getPage(), request.getPageSize(),
                request.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<RoleResponse>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<RoleResponse>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("role.page", request.getPage())
                            .put("role.size", request.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findAllRoles", "find_all_roles", attrs, () -> {
                        return roleRepository.findRoles(request)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<RoleResponse>> response = buildPaginatedResponse(
                                            pagedResult, request, "Roles retrieved successfully", RoleResponse::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                logger.info("Successfully retrieved {} roles",
                                                        pagedResult.getTotalRecords());
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findActivePaginated(FindAllRoles request) {
        String cacheKey = String.format("roles:active:%d:%d:%s", request.getPage(), request.getPageSize(),
                request.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<RoleResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<RoleResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("role.page", request.getPage())
                            .put("role.size", request.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findActiveRoles", "find_active_roles", attrs, () -> {
                        return roleRepository.findActiveRoles(request)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<RoleResponseDeleteAt>> response = buildPaginatedResponse(
                                            pagedResult, request, "Active roles retrieved successfully",
                                            RoleResponseDeleteAt::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                logger.info("Successfully retrieved {} active roles",
                                                        pagedResult.getTotalRecords());
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponsePagination<List<RoleResponseDeleteAt>>> findTrashedPaginated(FindAllRoles request) {
        String cacheKey = String.format("roles:trashed:%d:%d:%s", request.getPage(), request.getPageSize(),
                request.getSearch());

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        ApiResponsePagination<List<RoleResponseDeleteAt>> response = fromJson(cachedJson,
                                new TypeReference<ApiResponsePagination<List<RoleResponseDeleteAt>>>() {
                                });
                        return Uni.createFrom().item(response);
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder()
                            .put("role.page", request.getPage())
                            .put("role.size", request.getPageSize())
                            .build();

                    return tracingMetrics.traceAndMeasure("findTrashedRoles", "find_trashed_roles", attrs, () -> {
                        return roleRepository.findTrashedRoles(request)
                                .chain(pagedResult -> {
                                    ApiResponsePagination<List<RoleResponseDeleteAt>> response = buildPaginatedResponse(
                                            pagedResult, request, "Trashed roles retrieved successfully",
                                            RoleResponseDeleteAt::from);

                                    return redisService
                                            .setWithExpirationReactive(cacheKey, toJson(response),
                                                    LIST_CACHE_TTL_SECONDS)
                                            .map(v -> {
                                                logger.info("Cached response for key: {}", cacheKey);
                                                logger.info("Successfully retrieved {} trashed roles",
                                                        pagedResult.getTotalRecords());
                                                return response;
                                            });
                                });
                    });
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponse<RoleResponse>> findById(Long id) {
        String cacheKey = "role:" + id;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        RoleResponse cachedRole = fromJson(cachedJson, RoleResponse.class);
                        return Uni.createFrom().item(ApiResponse.success("Role found", cachedRole));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder().put("role.id", id).build();

                    return tracingMetrics.traceAndMeasure("findRoleById", "find_role_by_id", attrs, () -> {
                        return roleRepository.findById(id)
                                .chain(role -> {
                                    if (role == null) {
                                        logger.warn("Role not found with id: {}", id);
                                        throw new NotFoundException("Role not found with id: " + id);
                                    }

                                    RoleResponse roleResponse = RoleResponse.from(role);

                                    return redisService.setReactive(cacheKey, toJson(roleResponse))
                                            .map(v -> {
                                                logger.info("Cached role for key: {}", cacheKey);
                                                logger.info("Successfully found role with id: {} and name: {}", id,
                                                        role.getRoleName());
                                                return ApiResponse.success("Role found", roleResponse);
                                            });
                                });
                    });
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponse<List<RoleResponse>>> findByUserId(Long userId) {
        String cacheKey = "roles:user:" + userId;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        List<RoleResponse> cachedRoles = fromJson(cachedJson, new TypeReference<List<RoleResponse>>() {
                        });
                        return Uni.createFrom().item(ApiResponse.success("Roles found", cachedRoles));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder().put("user.id", userId).build();

                    return tracingMetrics.traceAndMeasure("findRolesByUserId", "find_role_by_user_id", attrs, () -> {
                        return roleRepository.findUserRoles(userId)
                                .chain(roles -> {
                                    List<RoleResponse> responses = roles.stream()
                                            .map(RoleResponse::from)
                                            .collect(Collectors.toList());

                                    return redisService.setReactive(cacheKey, toJson(responses))
                                            .map(v -> {
                                                logger.info("Cached roles for key: {}", cacheKey);
                                                logger.info("Successfully found {} roles for user id: {}",
                                                        responses.size(), userId);
                                                return ApiResponse.success("Roles found", responses);
                                            });
                                });
                    });
                });
    }

    @Override
    @WithSession
    public Uni<ApiResponse<RoleResponse>> findByName(String name) {
        String cacheKey = "role:name:" + name;

        return redisService.getReactive(cacheKey)
                .chain(cachedJson -> {
                    if (cachedJson != null) {
                        logger.info("Cache HIT for key: {}", cacheKey);
                        RoleResponse cachedRole = fromJson(cachedJson, RoleResponse.class);
                        return Uni.createFrom().item(ApiResponse.success("Role found", cachedRole));
                    }

                    logger.info("Cache MISS for key: {}. Fetching from DB.", cacheKey);
                    Attributes attrs = Attributes.builder().put("role.name", name).build();

                    return tracingMetrics.traceAndMeasure("findRoleByName", "find_role_by_name", attrs, () -> {
                        return roleRepository.findByRoleName(name)
                                .chain(role -> {
                                    if (role == null) {
                                        logger.warn("Role not found with name: {}", name);
                                        throw new NotFoundException("Role not found with name: " + name);
                                    }

                                    RoleResponse roleResponse = RoleResponse.from(role);

                                    return redisService.setReactive(cacheKey, toJson(roleResponse))
                                            .map(v -> {
                                                logger.info("Cached role for key: {}", cacheKey);
                                                logger.info("Successfully found role with name: {}",
                                                        role.getRoleName());
                                                return ApiResponse.success("Role found", roleResponse);
                                            });
                                });
                    });
                });
    }

    private <T, R> ApiResponsePagination<List<R>> buildPaginatedResponse(
            PagedResult<T> pagedResult,
            FindAllRoles request,
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