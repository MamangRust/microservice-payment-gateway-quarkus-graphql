package com.sanedge.gateway.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.AuthDto.ForgotPasswordRequest;
import com.sanedge.gateway.dto.AuthDto.GetMeResponse;
import com.sanedge.gateway.dto.AuthDto.LoginRequest;
import com.sanedge.gateway.dto.AuthDto.LoginResponse;
import com.sanedge.gateway.dto.AuthDto.RefreshTokenRequest;
import com.sanedge.gateway.dto.AuthDto.RefreshTokenResponse;
import com.sanedge.gateway.dto.AuthDto.RegisterRequest;
import com.sanedge.gateway.dto.AuthDto.RegisterResponse;
import com.sanedge.gateway.dto.AuthDto.ResetPasswordRequest;
import com.sanedge.gateway.dto.AuthDto.SimpleResponse;
import com.sanedge.gateway.dto.AuthDto.VerifyCodeRequest;
import com.sanedge.gateway.telemetry.TelemetryHelper;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    TelemetryHelper telemetryHelper;

    @Mock
    pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authService;

    AuthServiceImpl authServiceImpl;

    @BeforeEach
    void setUp() throws Exception {
        authServiceImpl = new AuthServiceImpl();

        setField(authServiceImpl, "telemetryHelper", telemetryHelper);
        setField(authServiceImpl, "authService", authService);

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
    void register_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.Auth.ApiResponseRegister responseProto = pb.Auth.ApiResponseRegister.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User registered successfully")
                .build();

        when(authService.registerUser(any(pb.Auth.RegisterRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RegisterRequest request = new RegisterRequest("John", "Doe", "john@example.com", "password123", "password123");
        RegisterResponse result = authServiceImpl.register(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("User registered successfully");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }

    @Test
    void login_returnsSuccess() {
        pb.Auth.TokenResponse tokenProto = pb.Auth.TokenResponse.newBuilder()
                .setAccessToken("access-token-123")
                .setRefreshToken("refresh-token-456")
                .build();

        pb.Auth.ApiResponseLogin responseProto = pb.Auth.ApiResponseLogin.newBuilder()
                .setData(tokenProto)
                .setStatus("success")
                .setMessage("Login successful")
                .build();

        when(authService.loginUser(any(pb.Auth.LoginRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        LoginRequest request = new LoginRequest("john@example.com", "password123");
        LoginResponse result = authServiceImpl.login(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().accessToken()).isEqualTo("access-token-123");
        assertThat(result.data().refreshToken()).isEqualTo("refresh-token-456");
    }

    @Test
    void verifyCode_returnsSuccess() {
        pb.Auth.ApiResponseVerifyCode responseProto = pb.Auth.ApiResponseVerifyCode.newBuilder()
                .setStatus("success")
                .setMessage("Code verified")
                .build();

        when(authService.verifyCode(any(pb.Auth.VerifyCodeRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        VerifyCodeRequest request = new VerifyCodeRequest("123456");
        SimpleResponse result = authServiceImpl.verify(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Code verified");
    }

    @Test
    void forgotPassword_returnsSuccess() {
        pb.Auth.ApiResponseForgotPassword responseProto = pb.Auth.ApiResponseForgotPassword.newBuilder()
                .setStatus("success")
                .setMessage("Reset email sent")
                .build();

        when(authService.forgotPassword(any(pb.Auth.ForgotPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ForgotPasswordRequest request = new ForgotPasswordRequest("john@example.com");
        SimpleResponse result = authServiceImpl.forgotPassword(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Reset email sent");
    }

    @Test
    void resetPassword_returnsSuccess() {
        pb.Auth.ApiResponseResetPassword responseProto = pb.Auth.ApiResponseResetPassword.newBuilder()
                .setStatus("success")
                .setMessage("Password reset successfully")
                .build();

        when(authService.resetPassword(any(pb.Auth.ResetPasswordRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        ResetPasswordRequest request = new ResetPasswordRequest("reset-token-123", "newPassword123", "newPassword123");
        SimpleResponse result = authServiceImpl.resetPassword(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.message()).isEqualTo("Password reset successfully");
    }

    @Test
    void refreshToken_returnsSuccess() {
        pb.Auth.TokenResponse tokenProto = pb.Auth.TokenResponse.newBuilder()
                .setAccessToken("new-access-token")
                .setRefreshToken("new-refresh-token")
                .build();

        pb.Auth.ApiResponseRefreshToken responseProto = pb.Auth.ApiResponseRefreshToken.newBuilder()
                .setData(tokenProto)
                .setStatus("success")
                .setMessage("Token refreshed")
                .build();

        when(authService.refreshToken(any(pb.Auth.RefreshTokenRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        RefreshTokenResponse result = authServiceImpl.refresh(request).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().accessToken()).isEqualTo("new-access-token");
    }

    @Test
    void getMe_returnsSuccess() {
        pb.user.User.UserResponse userProto = pb.user.User.UserResponse.newBuilder()
                .setId(1)
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@example.com")
                .setCreatedAt("2024-01-01T00:00:00Z")
                .setUpdatedAt("2024-01-01T00:00:00Z")
                .build();

        pb.Auth.ApiResponseGetMe responseProto = pb.Auth.ApiResponseGetMe.newBuilder()
                .setData(userProto)
                .setStatus("success")
                .setMessage("User profile retrieved")
                .build();

        when(authService.getMe(any(pb.Auth.GetMeRequest.class)))
                .thenReturn(Uni.createFrom().item(responseProto));

        GetMeResponse result = authServiceImpl.getMe(1).await().indefinitely();

        assertThat(result.status()).isEqualTo("success");
        assertThat(result.data()).isNotNull();
        assertThat(result.data().id()).isEqualTo(1);
        assertThat(result.data().email()).isEqualTo("john@example.com");
    }
}