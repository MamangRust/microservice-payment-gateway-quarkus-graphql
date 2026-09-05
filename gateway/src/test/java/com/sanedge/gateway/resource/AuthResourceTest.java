package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.AuthDto;
import com.sanedge.gateway.service.AuthService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock private AuthService authService;
    private AuthResource authResource;

    @BeforeEach
    void setUp() throws Exception {
        authResource = new AuthResource();
        Field f = AuthResource.class.getDeclaredField("authService");
        f.setAccessible(true);
        f.set(authResource, authService);
    }

    @Test
    void register_Success() {
        AuthDto.RegisterResponse dto = new AuthDto.RegisterResponse("success", "registered", null);
        lenient().when(authService.register(any())).thenReturn(Uni.createFrom().item(dto));
        AuthDto.RegisterResponse result = authResource.register(new AuthDto.RegisterRequest("John", "Doe", "u@e.com", "pwd", "pwd")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("registered");
    }

    @Test
    void login_Success() {
        AuthDto.LoginResponse dto = new AuthDto.LoginResponse("success", "logged in", null);
        lenient().when(authService.login(any())).thenReturn(Uni.createFrom().item(dto));
        AuthDto.LoginResponse result = authResource.login(new AuthDto.LoginRequest("u@e.com", "pwd")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void verify_Success() {
        AuthDto.SimpleResponse dto = new AuthDto.SimpleResponse("success", "verified");
        lenient().when(authService.verify(any())).thenReturn(Uni.createFrom().item(dto));
        AuthDto.SimpleResponse result = authResource.verify(new AuthDto.VerifyCodeRequest("ABC123")).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void forgotPassword_Success() {
        AuthDto.SimpleResponse dto = new AuthDto.SimpleResponse("success", "email sent");
        lenient().when(authService.forgotPassword(any())).thenReturn(Uni.createFrom().item(dto));
        AuthDto.SimpleResponse result = authResource.forgotPassword(new AuthDto.ForgotPasswordRequest("u@e.com")).await().indefinitely();
        assertThat(result.message()).isEqualTo("email sent");
    }

    @Test
    void getMe_Success() {
        AuthDto.GetMeResponse dto = new AuthDto.GetMeResponse("success", "me", null);
        lenient().when(authService.getMe(any(int.class))).thenReturn(Uni.createFrom().item(dto));
        AuthDto.GetMeResponse result = authResource.getMe(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }
}
