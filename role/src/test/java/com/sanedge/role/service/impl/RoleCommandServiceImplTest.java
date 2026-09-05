package com.sanedge.role.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.CreateRoleRequest;
import com.sanedge.role.domain.requests.UpdateRoleRequest;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;
import com.sanedge.role.entity.Role;
import com.sanedge.role.repository.RoleRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleCommandServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private com.sanedge.role.repository.UserRoleRepository userRoleRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    private RoleCommandServiceImpl roleCommandService;

    @BeforeEach
    void setUp() {
        roleCommandService = new RoleCommandServiceImpl(
                roleRepository,
                userRoleRepository,
                redisService,
                tracingMetrics);

        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
        lenient().doAnswer(invokeSupplier())
                .when(tracingMetrics)
                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());
    }

    private Role createMockRole(Long id, String roleName) {
        Role role = new Role();
        role.id = id != null ? id.longValue() : null;
        role.setRoleName(roleName);
        role.setDeletedAt(null);
        role.setCreatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()));
        role.setUpdatedAt(Timestamp.valueOf(java.time.LocalDateTime.now()));
        return role;
    }

    private com.sanedge.role.entity.UserRole createMockUserRole(Long userId, Long roleId) {
        com.sanedge.role.entity.UserRole userRole = new com.sanedge.role.entity.UserRole();
        userRole.setUserId(userId);
        userRole.setRole(createMockRole(roleId, "Role" + roleId));
        return userRole;
    }

    @Test
    void createRole_roleAlreadyExists_throwsResourceAlreadyExistsException() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("Admin");

        Role existingRole = createMockRole(1L, "Admin");

        when(roleRepository.findByRoleName("Admin")).thenReturn(Uni.createFrom().item(existingRole));

        try {
            roleCommandService.create(request).await().indefinitely();
            Assertions.fail("Expected ResourceAlreadyExistsException");
        } catch (ResourceAlreadyExistsException e) {
            assertThat(e.getMessage()).contains("already exists");
        }
    }

    @Test
    void createRole_success_createsNewRole() {
        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NewRole");

        lenient().when(roleRepository.findByRoleName("NewRole")).thenReturn(Uni.createFrom().nullItem());
        lenient().when(roleRepository.persist(any(Role.class))).thenAnswer(invocation -> {
            Role roleToPersist = invocation.getArgument(0);
            roleToPersist.id = 1L;
            return Uni.createFrom().item(roleToPersist);
        });
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleCommandService.create(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role created successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("NewRole");
    }

    @Test
    void updateRole_roleNotFound_throwsResourceNotFoundException() {
        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(999);
        request.setName("UpdatedRole");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().nullItem());

        try {
            roleCommandService.update(request).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Role not found with id: " + request.getRoleId());
        }
    }

    @Test
    void updateRole_nameAlreadyExists_throwsResourceAlreadyExistsException() {
        Role existingRole = createMockRole(1L, "OldRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("AnotherAdmin");

        Role conflictingRole = createMockRole(2L, "AnotherAdmin");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));
        lenient().when(roleRepository.findByRoleName("AnotherAdmin"))
                .thenReturn(Uni.createFrom().item(conflictingRole));

        try {
            roleCommandService.update(request).await().indefinitely();
            Assertions.fail("Expected ResourceAlreadyExistsException");
        } catch (ResourceAlreadyExistsException e) {
            assertThat(e.getMessage()).contains("already exists");
        }
    }

    @Test
    void updateRole_success_updatesRolename() {
        Role existingRole = createMockRole(1L, "OldRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("NewRoleName");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));
        lenient().when(roleRepository.findByRoleName("NewRoleName")).thenReturn(Uni.createFrom().nullItem());
        lenient().when(roleRepository.persist(any(Role.class))).thenReturn(Uni.createFrom().item(existingRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role updated successfully");
        assertThat(response.data().getName()).isEqualTo("NewRoleName");
    }

    @Test
    void updateRole_sameName_noOp_noException() {
        Role existingRole = createMockRole(1L, "SameRoleName");

        UpdateRoleRequest request = new UpdateRoleRequest();
        request.setRoleId(1);
        request.setName("SameRoleName");

        when(roleRepository.findById(anyLong())).thenReturn(Uni.createFrom().item(existingRole));

        ApiResponse<RoleResponse> response = roleCommandService.update(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role updated successfully");
        verify(roleRepository, never()).findByRoleName(anyString());
        verify(roleRepository, never()).persist(any(Role.class));
    }

    @Test
    void trashRole_roleNotFound_throwsResourceNotFoundException() {
        Long roleId = 999L;

        when(roleRepository.trash(roleId)).thenReturn(Uni.createFrom().nullItem());

        try {
            roleCommandService.trash(roleId).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Role not found with id: " + roleId);
        }
    }

    @Test
    void trashRole_success_trashRole() {
        Role trashedRole = createMockRole(1L, "TrashedRole");
        Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
        trashedRole.setDeletedAt(now);

        when(roleRepository.trash(anyLong())).thenReturn(Uni.createFrom().item(trashedRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.trash(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role trashed successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("TrashedRole");
    }

    @Test
    void restoreRole_roleNotTrashed_throwsResourceNotFoundException() {
        Long roleId = 1L;

        when(roleRepository.restore(roleId)).thenReturn(Uni.createFrom().nullItem());

        try {
            roleCommandService.restore(roleId).await().indefinitely();
            Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).contains("must be trashed first");
        }
    }

    @Test
    void restoreRole_success_restoreRole() {
        Role restoredRole = createMockRole(1L, "RestoredRole");

        when(roleRepository.restore(anyLong())).thenReturn(Uni.createFrom().item(restoredRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<RoleResponseDeleteAt> response = roleCommandService.restore(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role restored successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getName()).isEqualTo("RestoredRole");
    }

    @Test
    void deletePermanent_roleNotFoundOrNotTrashed_throwsInvalidRequestException() {
        Long roleId = 999L;

        when(roleRepository.deletePermanent(roleId)).thenReturn(Uni.createFrom().nullItem());

        try {
            roleCommandService.deletePermanent(roleId).await().indefinitely();
            Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).contains("must be trashed");
        }
    }

    @Test
    void deletePermanent_success_deleteRole() {
        Role deletedRole = createMockRole(1L, "DeletedRole");
        Timestamp now = Timestamp.valueOf(java.time.LocalDateTime.now());
        deletedRole.setDeletedAt(now);

        when(roleRepository.deletePermanent(anyLong())).thenReturn(Uni.createFrom().item(deletedRole));
        when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.deletePermanent(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role deleted permanently");
    }

    @Test
    void restoreAllTrashedRoles_noTrashedRoles_throwsResourceNotFoundException() {
        when(roleRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            roleCommandService.restoreAllTrashedRoles().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed roles found");
        }
    }

    @Test
    void restoreAllTrashedRoles_success_restoreAll() {
        when(roleRepository.restoreAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.restoreAllTrashedRoles().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).contains("have been restored");
    }

    @Test
    void deleteAllTrashedRoles_noTrashedRoles_throwsResourceNotFoundException() {
        when(roleRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(false));

        try {
            roleCommandService.deleteAllTrashedRoles().await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("No trashed roles found");
        }
    }

    @Test
    void deleteAllTrashedRoles_success_deleteAll() {
        when(roleRepository.deleteAllDeleted()).thenReturn(Uni.createFrom().item(true));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.deleteAllTrashedRoles().await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).contains("deleted permanently");
    }

    @Test
    void assignRoleToUser_success_assignsRole() {
        Long userId = 1L;
        Long roleId = 2L;

        com.sanedge.role.entity.UserRole userRole = createMockUserRole(userId, roleId);

        when(userRoleRepository.assignRole(anyLong(), anyLong(), any(com.sanedge.role.repository.RoleRepository.class)))
                .thenReturn(Uni.createFrom().item(userRole));

        lenient().when(roleRepository.findById(anyLong()))
                .thenReturn(Uni.createFrom().item(createMockRole(roleId, "Role")));

        ApiResponse<UserRoleResponse> response = roleCommandService.assignRoleToUser(1L, 2L)
                .await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role assigned to user successfully");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void removeRoleFromUser_notFound_throwsResourceNotFoundException() {
        Long userId = 999L;
        Long roleId = 888L;

        when(userRoleRepository.removeRole(userId, roleId)).thenReturn(Uni.createFrom().item(false));

        try {
            roleCommandService.removeRoleFromUser(userId, roleId).await().indefinitely();
            Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("UserRole association not found");
        }
    }

    @Test
    void removeRoleFromUser_success_removesRole() {
        when(userRoleRepository.removeRole(anyLong(), anyLong())).thenReturn(Uni.createFrom().item(true));
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = roleCommandService.removeRoleFromUser(1L, 2L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("Role removed from user successfully");
    }

    private Answer<Uni<?>> invokeSupplier() {
        return invocation -> {
            Supplier<?> supplier = null;
            for (Object arg : invocation.getArguments()) {
                if (arg instanceof Supplier<?>) {
                    supplier = (Supplier<?>) arg;
                    break;
                }
            }
            return supplier != null ? (Uni<?>) supplier.get() : null;
        };
    }
}