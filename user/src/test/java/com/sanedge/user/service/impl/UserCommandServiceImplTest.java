package com.sanedge.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.PasswordUtil;
import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.domain.requests.UpdateUserRequest;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;
import com.sanedge.user.entity.User;
import com.sanedge.user.repository.UserRepository;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import pb.role.RoleService;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private RoleService roleService;

    private UserCommandServiceImpl userCommandService;

    @BeforeEach
    void setUp() {
        userCommandService = new UserCommandServiceImpl(
                userRepository,
                passwordUtil,
                redisService,
                tracingMetrics);

        try {
            java.lang.reflect.Field roleQueryField = UserCommandServiceImpl.class.getDeclaredField("roleQueryService");
            roleQueryField.setAccessible(true);
            roleQueryField.set(userCommandService, roleService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        lenient().doAnswer(invocation -> {
            Supplier<Uni<?>> supplier = invocation.getArgument(3);
            return supplier.get();
        }).when(tracingMetrics)
                .traceAndMeasure(
                        anyString(),
                        anyString(),
                        any(Attributes.class),
                        any());
    }

    @Test
    void createUser_passwordMismatch_throwsInvalidRequestException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("password123");
        request.setConfirmPassword("differentpassword");
        request.setEmail("test@example.com");
        request.setFirstname("John");
        request.setLastname("Doe");

        try {
            userCommandService.createUser(request).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Passwords do not match");
        }
    }

    @Test
    void createUser_usernameAlreadyExists_throwsResourceAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setEmail("test@example.com");
        request.setFirstname("John");
        request.setLastname("Doe");

        when(userRepository.existsByUsername("existinguser")).thenReturn(Uni.createFrom().item(true));

        try {
            userCommandService.createUser(request).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceAlreadyExistsException");
        } catch (ResourceAlreadyExistsException e) {
            assertThat(e.getMessage()).isEqualTo("Username already exists");
        }
    }

    @Test
    void createUser_emailAlreadyExists_throwsResourceAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setEmail("existing@example.com");
        request.setFirstname("John");
        request.setLastname("Doe");

        when(userRepository.existsByUsername("newuser")).thenReturn(Uni.createFrom().item(false));
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(Uni.createFrom().item(true));

        try {
            userCommandService.createUser(request).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceAlreadyExistsException");
        } catch (ResourceAlreadyExistsException e) {
            assertThat(e.getMessage()).isEqualTo("Email already exists");
        }
    }

    @Test
    void createUser_success_withoutRoles_createsDefaultUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setConfirmPassword("password123");
        request.setEmail("newuser@example.com");
        request.setFirstname("John");
        request.setLastname("Doe");

        pb.role.RoleQuery.FindByNameRoleRequest requestProto = pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                .setName("ROLE_USER")
                .build();
        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setStatus("success")
                .setMessage("Role found")
                .setData(pb.role.Role.RoleResponse.newBuilder()
                        .setId(1)
                        .setName("ROLE_USER")
                        .build())
                .build();

        lenient().when(roleService.findByNameRole(requestProto)).thenReturn(Uni.createFrom().item(responseProto));

        lenient().when(userRepository.persist(any(User.class))).thenAnswer(invocation -> {
            User userToPersist = invocation.getArgument(0);
            userToPersist.id = 1L;
            return Uni.createFrom().item(userToPersist);
        });
        when(userRepository.existsByUsername("newuser")).thenReturn(Uni.createFrom().item(false));
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(Uni.createFrom().item(false));
        lenient().when(passwordUtil.hashPassword("password123")).thenReturn("hashedPassword");
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<UserResponse> response = userCommandService.createUser(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User registered successfully");
        assertThat(response.data()).isNotNull();
        assertThat(response.data().getUsername()).isEqualTo("newuser");
    }

    @Test
    void updateUser_userNotFound_throwsResourceNotFoundException() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(999)
                .firstname("Updated")
                .build();

        when(userRepository.findById(999)).thenReturn(Uni.createFrom().nullItem());

        try {
            userCommandService.updateUser(request).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("User not found");
        }
    }

    @Test
    void updateUser_success_updatesUser() {
        User existingUser = new User();
        existingUser.id = 1L;
        existingUser.setUsername("oldusername");
        existingUser.setEmail("old@example.com");
        existingUser.setFirstname("Old");
        existingUser.setLastname("Name");
        existingUser.setPassword("hashedPassword");
        existingUser.setRoles(new HashSet<>());
        existingUser.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));

        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(1)
                .firstname("Updated")
                .lastname("User")
                .build();

        when(userRepository.findById(1)).thenReturn(Uni.createFrom().item(existingUser));
        lenient().when(userRepository.persist(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return Uni.createFrom().item(user);
        });
        lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<UserResponse> response = userCommandService.updateUser(request).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User updated successfully");
    }

    @Test
    void updateUser_passwordMismatch_throwsInvalidRequestException() {
        User existingUser = new User();
        existingUser.id = 1L;
        existingUser.setUsername("testuser");
        existingUser.setEmail("test@example.com");
        existingUser.setFirstname("John");
        existingUser.setLastname("Doe");
        existingUser.setPassword("hashedPassword");
        existingUser.setRoles(new HashSet<>());

        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(1)
                .password("newpassword")
                .confirmPassword("differentpassword")
                .build();

        when(userRepository.findById(1)).thenReturn(Uni.createFrom().item(existingUser));

        try {
            userCommandService.updateUser(request).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("Passwords do not match");
        }
    }

    @Test
    void trashUser_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.trash(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            userCommandService.trashed(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected ResourceNotFoundException");
        } catch (ResourceNotFoundException e) {
            assertThat(e.getMessage()).contains("Trashed user not found");
        }
    }

    @Test
    void trashUser_success_trashesUser() {
        User trashedUser = new User();
        trashedUser.id = 1L;
        trashedUser.setUsername("testuser");
        trashedUser.setEmail("test@example.com");
        trashedUser.setFirstname("John");
        trashedUser.setLastname("Doe");
        trashedUser.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(userRepository.trash(1L)).thenReturn(Uni.createFrom().item(trashedUser));
        lenient().when(redisService.deleteReactive("user:1")).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<UserResponseDeleteAt> response = userCommandService.trashed(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User trashed successfully");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void restoreUser_userNotFound_throwsInvalidRequestException() {
        when(userRepository.restore(999L)).thenReturn(Uni.createFrom().nullItem());

        try {
            userCommandService.restore(999L).await().indefinitely();
            org.junit.jupiter.api.Assertions.fail("Expected InvalidRequestException");
        } catch (InvalidRequestException e) {
            assertThat(e.getMessage()).isEqualTo("User not found or must be trashed first");
        }
    }

    @Test
    void restoreUser_success_restoresUser() {
        User restoredUser = new User();
        restoredUser.id = 1L;
        restoredUser.setUsername("testuser");
        restoredUser.setEmail("test@example.com");
        restoredUser.setFirstname("John");
        restoredUser.setLastname("Doe");

        when(userRepository.restore(1L)).thenReturn(Uni.createFrom().item(restoredUser));
        when(redisService.deleteReactive("user:1")).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<UserResponseDeleteAt> response = userCommandService.restore(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User restored successfully");
        assertThat(response.data()).isNotNull();
    }

    @Test
    void deletePermanent_success_deletesUser() {
        User deletedUser = new User();
        deletedUser.id = 1L;
        deletedUser.setUsername("testuser");

        when(userRepository.deletePermanent(1L)).thenReturn(Uni.createFrom().item(deletedUser));
        when(redisService.deleteReactive("user:1")).thenReturn(Uni.createFrom().voidItem());

        ApiResponse<Void> response = userCommandService.deletePermanent(1L).await().indefinitely();

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.message()).isEqualTo("User deleted permanently");
    }
}
