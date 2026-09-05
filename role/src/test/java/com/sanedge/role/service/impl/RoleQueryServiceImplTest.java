package com.sanedge.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.domain.response.ApiResponsePagination;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.FindAllRoles;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private RoleQueryServiceImpl service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new RoleQueryServiceImpl(roleRepository, redisService, tracingMetrics, objectMapper);

        // Lenient stubs to execute the supplier directly (3-arg traceAndMeasure)
        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Role createMockRole(Long id) {
        Role role = new Role();
        role.id = id != null ? id.longValue() : null;
        role.setRoleName("Admin");
        role.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        role.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        return role;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private FindAllRoles findAllReq(int page, int size, String search) {
        FindAllRoles req = new FindAllRoles();
        req.setPage(page);
        req.setPageSize(size);
        req.setSearch(search == null ? "" : search);
        return req;
    }

    // ------------------ findAllPaginated ------------------
    @Nested
    @DisplayName("findAllPaginated tests")
    class FindAllPaginatedTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllRoles req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findRoles(any(FindAllRoles.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockRole(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<RoleResponse>> result = service.findAllPaginated(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).getName()).isEqualTo("Admin");
        }
        @Test void cacheHit_returnsCached() {
            FindAllRoles req = findAllReq(1, 10, "");
            ApiResponsePagination<List<RoleResponse>> cached = new ApiResponsePagination<>(
                    "success", "Roles retrieved successfully",
                    List.of(RoleResponse.from(createMockRole(1L))), null);
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cached)));

            ApiResponsePagination<List<RoleResponse>> result = service.findAllPaginated(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    // ------------------ findActivePaginated ------------------
    @Nested
    @DisplayName("findActivePaginated tests")
    class FindActivePaginatedTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllRoles req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findActiveRoles(any(FindAllRoles.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockRole(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<RoleResponseDeleteAt>> result = service.findActivePaginated(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    // ------------------ findTrashedPaginated ------------------
    @Nested
    @DisplayName("findTrashedPaginated tests")
    class FindTrashedPaginatedTests {
        @Test void cacheMiss_fetchesFromDb() {
            FindAllRoles req = findAllReq(1, 10, "");
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findTrashedRoles(any(FindAllRoles.class)))
                    .thenReturn(Uni.createFrom().item(new PagedResult<>(List.of(createMockRole(1L)), 1)));
            when(redisService.setWithExpirationReactive(anyString(), anyString(), anyLong()))
                    .thenReturn(Uni.createFrom().voidItem());

            ApiResponsePagination<List<RoleResponseDeleteAt>> result = service.findTrashedPaginated(req).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    // ------------------ findById ------------------
    @Nested
    @DisplayName("findById tests")
    class FindByIdTests {
        @Test void cacheMiss_fetchesFromDb() {
            Long id = 1L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(createMockRole(id)));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<RoleResponse> result = service.findById(id).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getId()).isEqualTo(id.intValue());
        }
        @Test void notFound_throwsException() {
            Long id = 999L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

            try {
                service.findById(id).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Role not found");
            }
        }
    }

    // ------------------ findByUserId ------------------
    @Nested
    @DisplayName("findByUserId tests")
    class FindByUserIdTests {
        @Test void cacheMiss_fetchesFromDb() {
            Long userId = 100L;
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findUserRoles(anyLong()))
                    .thenReturn(Uni.createFrom().item(List.of(createMockRole(1L), createMockRole(2L))));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<List<RoleResponse>> result = service.findByUserId(userId).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(2);
        }
        @Test void cacheHit_returnsCached() {
            Long userId = 100L;
            List<RoleResponse> cachedData = List.of(RoleResponse.from(createMockRole(1L)));
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().item(toJson(cachedData)));

            ApiResponse<List<RoleResponse>> result = service.findByUserId(userId).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data()).hasSize(1);
        }
    }

    // ------------------ findByName ------------------
    @Nested
    @DisplayName("findByName tests")
    class FindByNameTests {
        @Test void cacheMiss_fetchesFromDb() {
            String name = "Admin";
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().item(createMockRole(1L)));
            when(redisService.setReactive(anyString(), anyString())).thenReturn(Uni.createFrom().voidItem());

            ApiResponse<RoleResponse> result = service.findByName(name).await().indefinitely();
            assertThat(result.status()).isEqualTo("success");
            assertThat(result.data().getName()).isEqualTo("Admin");
        }
        @Test void notFound_throwsException() {
            String name = "NonExistent";
            when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
            when(roleRepository.findByRoleName(anyString())).thenReturn(Uni.createFrom().nullItem());

            try {
                service.findByName(name).await().indefinitely();
                org.assertj.core.api.Assertions.fail("Expected NotFoundException");
            } catch (NotFoundException e) {
                assertThat(e.getMessage()).contains("Role not found");
            }
        }
    }
}