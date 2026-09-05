package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.UserDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

    @Mock
    pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub userCommandService;

    UserServiceImpl userService;

    @BeforeEach
    void setUp() throws Exception {
        userService = new UserServiceImpl();

        setField(userService, "telemetryHelper", telemetryHelper);
        setField(userService, "userQueryService", userQueryService);
        setField(userService, "userCommandService", userCommandService);

        lenient().when(telemetryHelper.traceAndMetric(anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Uni<?>> supplier = inv.getArgument(1);
                    return supplier.get();
                });
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void listUsers_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.user.UserQuery.ApiResponsePaginationUser responseProto = pb.user.UserQuery.ApiResponsePaginationUser.newBuilder()
                .addData(userProto)
                .setStatus("success")
                .setMessage("Users found")
                .build();

        when(userQueryService.findAll(any(pb.user.User.FindAllUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllUserResponse result = userService.listUsers(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).email()).isEqualTo("john@example.com");
    }

    @Test
    void getUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User found")
                .build();

        when(userQueryService.findById(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdUserResponse result = userService.getUser(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }

    @Test
    void createUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User created")
                .build();

        when(userCommandService.create(any(pb.user.UserCommand.CreateUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateUserRequest request = new CreateUserRequest("John", "Doe", "john@example.com", "password123", "password123");
        CreateUserResponse result = userService.createUser(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }

    @Test
    void updateUser_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Smith")
                .setEmail("johnsmith@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.user.User.ApiResponseUser responseProto = pb.user.User.ApiResponseUser.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User updated")
                .build();

        when(userCommandService.update(any(pb.user.UserCommand.UpdateUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateUserRequest request = new UpdateUserRequest("John", "Smith", "johnsmith@example.com", "newpassword123", "newpassword123");
        UpdateUserResponse result = userService.updateUser(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().lastname()).isEqualTo("Smith");
    }

    @Test
    void deleteUser_returnsSuccess() {
        pb.user.User.UserResponseDeleteAt userProto = pb.user.User.UserResponseDeleteAt.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.user.User.ApiResponseUserDeleteAt responseProto = pb.user.User.ApiResponseUserDeleteAt.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User deleted")
                .build();

        when(userCommandService.trashedUser(any(pb.user.User.FindByIdUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedUserResponse result = userService.deleteUser(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }
}