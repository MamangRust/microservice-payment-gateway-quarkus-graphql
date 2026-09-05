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

import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

    @Mock private UserService userService;
    private UserResource userResource;

    @BeforeEach
    void setUp() throws Exception {
        userResource = new UserResource();
        Field f = UserResource.class.getDeclaredField("userService");
        f.setAccessible(true);
        f.set(userResource, userService);
    }

    @Test
    void listUsers_Success() {
        UserDto.FindAllUserResponse dto = new UserDto.FindAllUserResponse(List.of(), "success", "ok");
        lenient().when(userService.listUsers(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        UserDto.FindAllUserResponse result = userResource.listUsers(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getUser_Success() {
        UserDto.FindByIdUserResponse dto = new UserDto.FindByIdUserResponse(null, "success", "ok");
        lenient().when(userService.getUser(anyInt())).thenReturn(Uni.createFrom().item(dto));
        UserDto.FindByIdUserResponse result = userResource.getUser(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createUser_Success() {
        UserDto.CreateUserResponse dto = new UserDto.CreateUserResponse(null, "success", "created");
        lenient().when(userService.createUser(any())).thenReturn(Uni.createFrom().item(dto));
        UserDto.CreateUserResponse result = userResource.createUser(new UserDto.CreateUserRequest("John", "Doe", "j@d.com", "p", "p")).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }

    @Test
    void deleteUser_Success() {
        UserDto.TrashedUserResponse dto = new UserDto.TrashedUserResponse(null, "success", "deleted");
        lenient().when(userService.deleteUser(anyInt())).thenReturn(Uni.createFrom().item(dto));
        UserDto.TrashedUserResponse result = userResource.deleteUser(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
