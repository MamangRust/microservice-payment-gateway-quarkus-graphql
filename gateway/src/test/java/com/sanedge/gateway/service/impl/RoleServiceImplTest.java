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

import com.sanedge.gateway.dto.RoleDto.*;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.role.MutinyRoleServiceGrpc.MutinyRoleServiceStub roleQueryService;

    @Mock
    pb.role.MutinyRoleCommandServiceGrpc.MutinyRoleCommandServiceStub roleCommandService;

    RoleServiceImpl roleService;

    @BeforeEach
    void setUp() throws Exception {
        roleService = new RoleServiceImpl();

        setField(roleService, "telemetryHelper", telemetryHelper);
        setField(roleService, "roleQueryService", roleQueryService);
        setField(roleService, "roleCommandService", roleCommandService);

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
    void listRoles_returnsSuccess() {
        pb.role.Role.RoleResponse protoRole = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("ROLE_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.role.RoleQuery.ApiResponsePaginationRole responseProto = pb.role.RoleQuery.ApiResponsePaginationRole.newBuilder()
                .addData(protoRole)
                .setStatus("success")
                .setMessage("Roles found")
                .build();

        when(roleQueryService.findAllRole(any(pb.role.Role.FindAllRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindAllRoleResponse result = roleService.listRoles(1, 10, "").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getRole_returnsSuccess() {
        pb.role.Role.RoleResponse protoRole = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("ROLE_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role found")
                .build();

        when(roleQueryService.findByIdRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdRoleResponse result = roleService.getRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getRoleByName_returnsSuccess() {
        pb.role.Role.RoleResponse protoRole = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("ROLE_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role found")
                .build();

        when(roleQueryService.findByNameRole(any(pb.role.RoleQuery.FindByNameRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        FindByIdRoleResponse result = roleService.getRoleByName("ROLE_ADMIN").await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void createRole_returnsSuccess() {
        pb.role.Role.RoleResponse protoRole = pb.role.Role.RoleResponse.newBuilder()
                .setId(2)
                .setName("ROLE_USER")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role created")
                .build();

        when(roleCommandService.createRole(any(pb.role.RoleCommand.CreateRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        CreateRoleRequest request = new CreateRoleRequest("ROLE_USER");
        CreateRoleResponse result = roleService.createRole(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("ROLE_USER");
    }

    @Test
    void updateRole_returnsSuccess() {
        pb.role.Role.RoleResponse protoRole = pb.role.Role.RoleResponse.newBuilder()
                .setId(1)
                .setName("ROLE_SUPER_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRole responseProto = pb.role.Role.ApiResponseRole.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role updated")
                .build();

        when(roleCommandService.updateRole(any(pb.role.RoleCommand.UpdateRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        UpdateRoleRequest request = new UpdateRoleRequest("ROLE_SUPER_ADMIN");
        UpdateRoleResponse result = roleService.updateRole(1, request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().name()).isEqualTo("ROLE_SUPER_ADMIN");
    }

    @Test
    void deleteRole_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt protoRole = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("ROLE_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRoleDeleteAt responseProto = pb.role.Role.ApiResponseRoleDeleteAt.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role deleted")
                .build();

        when(roleCommandService.trashedRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedRoleResponse result = roleService.deleteRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
    }

    @Test
    void restoreRole_returnsSuccess() {
        pb.role.Role.RoleResponseDeleteAt protoRole = pb.role.Role.RoleResponseDeleteAt.newBuilder()
                .setId(1)
                .setName("ROLE_ADMIN")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-02T00:00:00Z")
                .build();

        pb.role.Role.ApiResponseRoleDeleteAt responseProto = pb.role.Role.ApiResponseRoleDeleteAt.newBuilder()
                .setData(protoRole)
                .setStatus("success")
                .setMessage("Role restored")
                .build();

        when(roleCommandService.restoreRole(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        TrashedRoleResponse result = roleService.restoreRole(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
    }

    @Test
    void deleteRolePermanent_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseRoleDelete responseProto = pb.role.RoleCommand.ApiResponseRoleDelete.newBuilder()
                .setStatus("success")
                .setMessage("Role permanently deleted")
                .build();

        when(roleCommandService.deleteRolePermanent(any(pb.role.Role.FindByIdRoleRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = roleService.deleteRolePermanent(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Role permanently deleted");
    }

    @Test
    void restoreAllRoles_returnsSuccess() {
        pb.role.RoleCommand.ApiResponseRoleAll responseProto = pb.role.RoleCommand.ApiResponseRoleAll.newBuilder()
                .setStatus("success")
                .setMessage("All roles restored")
                .build();

        when(roleCommandService.restoreAllRole(any(com.google.protobuf.Empty.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        SimpleStatusMessageResponse result = roleService.restoreAllRoles().await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("All roles restored");
    }

    @Test
    void assignRoleToUser_returnsSuccess() {
        pb.role.RoleCommand.UserRoleResponse userRoleProto = pb.role.RoleCommand.UserRoleResponse.newBuilder()
                .setUserRoleId(1)
                .setUserId(1)
                .setRoleId(2)
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.role.RoleCommand.ApiResponseUserRole responseProto = pb.role.RoleCommand.ApiResponseUserRole.newBuilder()
                .setData(userRoleProto)
                .setStatus("success")
                .setMessage("Role assigned")
                .build();

        when(roleCommandService.assignRoleToUser(any(pb.role.RoleCommand.AssignRoleToUserRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        AssignRoleToUserResponse result = roleService.assignRoleToUser(1, 2).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().userId()).isEqualTo(1);
        assertThat(result.data().roleId()).isEqualTo(2);
    }
}