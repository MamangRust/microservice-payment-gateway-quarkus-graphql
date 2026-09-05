package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.RoleDto;
import com.sanedge.gateway.service.RoleService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class RoleResourceTest {

    @Mock private RoleService roleService;
    private RoleResource roleResource;

    @BeforeEach
    void setUp() throws Exception {
        roleResource = new RoleResource();
        Field f = RoleResource.class.getDeclaredField("roleService");
        f.setAccessible(true);
        f.set(roleResource, roleService);
    }

    @Test
    void listRoles_Success() {
        RoleDto.FindAllRoleResponse dto = new RoleDto.FindAllRoleResponse(List.of(), "success", "ok");
        lenient().when(roleService.listRoles(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        RoleDto.FindAllRoleResponse result = roleResource.listRoles(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getRole_Success() {
        RoleDto.FindByIdRoleResponse dto = new RoleDto.FindByIdRoleResponse(null, "success", "ok");
        lenient().when(roleService.getRole(anyInt())).thenReturn(Uni.createFrom().item(dto));
        RoleDto.FindByIdRoleResponse result = roleResource.getRole(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createRole_Success() {
        RoleDto.CreateRoleResponse dto = new RoleDto.CreateRoleResponse(null, "success", "created");
        lenient().when(roleService.createRole(any())).thenReturn(Uni.createFrom().item(dto));
        RoleDto.CreateRoleResponse result = roleResource.createRole(new RoleDto.CreateRoleRequest("ROLE_USER")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteRole_Success() {
        RoleDto.TrashedRoleResponse dto = new RoleDto.TrashedRoleResponse(null, "success", "deleted");
        lenient().when(roleService.deleteRole(anyInt())).thenReturn(Uni.createFrom().item(dto));
        RoleDto.TrashedRoleResponse result = roleResource.deleteRole(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
