package com.sanedge.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.auth.service.AuthService;

import io.smallrye.mutiny.Uni;
import pb.Auth;
import pb.Auth.ApiResponseVerifyCode;
import pb.Auth.VerifyCodeRequest;
import pb.Auth.ApiResponseForgotPassword;
import pb.Auth.ForgotPasswordRequest;
import pb.Auth.ApiResponseResetPassword;
import pb.Auth.ApiResponseRegister;
import pb.Auth.RegisterRequest;
import pb.user.User.UserResponse;
import pb.Auth.ApiResponseLogin;
import pb.Auth.LoginRequest;
import pb.Auth.TokenResponse;
import pb.Auth.ApiResponseRefreshToken;
import pb.Auth.RefreshTokenRequest;
import pb.Auth.ApiResponseGetMe;
import pb.Auth.GetMeRequest;

@ExtendWith(MockitoExtension.class)
class AuthGrpcHandlerTest {

        @Mock
        AuthService authService;

        @InjectMocks
        AuthGrpcHandler handler;

        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
                userResponse = UserResponse.newBuilder()
                                .setId(1)
                                .setFirstname("John")
                                .setLastname("Doe")
                                .setEmail("john@example.com")
                                .build();
        }

        @Test
        void registerUser_returnsSuccessResponse() {
                when(authService.register(any()))
                                .thenReturn(Uni.createFrom().item(userResponse));

                RegisterRequest request = RegisterRequest.newBuilder()
                                .setFirstname("John")
                                .setLastname("Doe")
                                .setEmail("john@example.com")
                                .setPassword("SecurePass123!")
                                .build();

                Auth.ApiResponseRegister response = handler.registerUser(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getEmail()).isEqualTo("john@example.com");
        }

        @Test
        void registerUser_returnsFailureResponse_onError() {
                when(authService.register(any()))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Email already exists")));

                RegisterRequest request = RegisterRequest.newBuilder()
                                .setEmail("existing@example.com")
                                .setPassword("Pass123!")
                                .build();

                Auth.ApiResponseRegister response = handler.registerUser(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("already exists");
        }

        @Test
        void loginUser_returnsSuccessWithTokens() {
                when(authService.login("john@example.com", "SecurePass123!"))
                                .thenReturn(Uni.createFrom().item(new String[] { "access-token", "refresh-token" }));

                LoginRequest request = LoginRequest.newBuilder()
                                .setEmail("john@example.com")
                                .setPassword("SecurePass123!")
                                .build();

                Auth.ApiResponseLogin response = handler.loginUser(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getAccessToken()).isEqualTo("access-token");
                assertThat(response.getData().getRefreshToken()).isEqualTo("refresh-token");
        }

        @Test
        void loginUser_returnsFailure_onInvalidCredentials() {
                when(authService.login("john@example.com", "wrong"))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Invalid credentials")));

                LoginRequest request = LoginRequest.newBuilder()
                                .setEmail("john@example.com")
                                .setPassword("wrong")
                                .build();

                Auth.ApiResponseLogin response = handler.loginUser(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("Invalid credentials");
        }

        @Test
        void loginUser_returnsFailure_onLockedAccount() {
                when(authService.login("locked@example.com", "any"))
                                .thenReturn(Uni.createFrom().failure(
                                                new RuntimeException(
                                                                "Account is locked due to too many failed attempts")));

                LoginRequest request = LoginRequest.newBuilder()
                                .setEmail("locked@example.com")
                                .setPassword("any")
                                .build();

                Auth.ApiResponseLogin response = handler.loginUser(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("locked");
        }

        @Test
        void refreshToken_returnsSuccessWithTokens() {
                when(authService.refresh("valid-refresh-token"))
                                .thenReturn(Uni.createFrom()
                                                .item(new String[] { "new-access-token", "new-refresh-token" }));

                RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                                .setRefreshToken("valid-refresh-token")
                                .build();

                Auth.ApiResponseRefreshToken response = handler.refreshToken(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getAccessToken()).isEqualTo("new-access-token");
        }

        @Test
        void refreshToken_returnsFailure_onInvalidToken() {
                when(authService.refresh("expired-token"))
                                .thenReturn(Uni.createFrom()
                                                .failure(new RuntimeException("Invalid or expired refresh token")));

                RefreshTokenRequest request = RefreshTokenRequest.newBuilder()
                                .setRefreshToken("expired-token")
                                .build();

                Auth.ApiResponseRefreshToken response = handler.refreshToken(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("expired");
        }

        @Test
        void getMe_returnsUserProfile() {
                when(authService.getMe(1L))
                                .thenReturn(Uni.createFrom().item(userResponse));

                GetMeRequest request = GetMeRequest.newBuilder()
                                .setUserId(1)
                                .build();

                Auth.ApiResponseGetMe response = handler.getMe(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.hasData()).isTrue();
                assertThat(response.getData().getEmail()).isEqualTo("john@example.com");
                assertThat(response.getData().getFirstname()).isEqualTo("John");
        }

        @Test
        void getMe_returnsFailure_whenUserNotFound() {
                when(authService.getMe(999L))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("User not found")));

                GetMeRequest request = GetMeRequest.newBuilder()
                                .setUserId(999)
                                .build();

                Auth.ApiResponseGetMe response = handler.getMe(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("not found");
        }

        @Test
        void forgotPassword_returnsSuccess() {
                when(authService.forgotPassword("john@example.com"))
                                .thenReturn(Uni.createFrom().voidItem());

                ForgotPasswordRequest request = ForgotPasswordRequest.newBuilder()
                                .setEmail("john@example.com")
                                .build();

                Auth.ApiResponseForgotPassword response = handler.forgotPassword(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).contains("Password reset email sent");
        }

        @Test
        void forgotPassword_returnsFailure_whenUserNotFound() {
                when(authService.forgotPassword("unknown@example.com"))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("User not found")));

                ForgotPasswordRequest request = ForgotPasswordRequest.newBuilder()
                                .setEmail("unknown@example.com")
                                .build();

                Auth.ApiResponseForgotPassword response = handler.forgotPassword(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("not found");
        }

        @Test
        void resetPassword_returnsSuccess() {
                when(authService.resetPassword(any()))
                                .thenReturn(Uni.createFrom().voidItem());

                pb.Auth.ResetPasswordRequest request = pb.Auth.ResetPasswordRequest.newBuilder()
                                .setResetToken("valid-token")
                                .setPassword("NewPass123!")
                                .setConfirmPassword("NewPass123!")
                                .build();

                Auth.ApiResponseResetPassword response = handler.resetPassword(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).contains("Password reset");
        }

        @Test
        void resetPassword_returnsFailure_onExpiredToken() {
                when(authService.resetPassword(any()))
                                .thenReturn(Uni.createFrom()
                                                .failure(new RuntimeException("Invalid or expired reset token")));

                pb.Auth.ResetPasswordRequest request = pb.Auth.ResetPasswordRequest.newBuilder()
                                .setResetToken("expired-token")
                                .setPassword("NewPass123!")
                                .setConfirmPassword("NewPass123!")
                                .build();

                Auth.ApiResponseResetPassword response = handler.resetPassword(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("expired");
        }

        @Test
        void verifyCode_returnsSuccess() {
                when(authService.verifyEmailByCode("ABC123"))
                                .thenReturn(Uni.createFrom().voidItem());

                VerifyCodeRequest request = VerifyCodeRequest.newBuilder()
                                .setCode("ABC123")
                                .build();

                Auth.ApiResponseVerifyCode response = handler.verifyCode(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("success");
                assertThat(response.getMessage()).contains("verified");
        }

        @Test
        void verifyCode_returnsFailure_onInvalidCode() {
                when(authService.verifyEmailByCode("INVALID"))
                                .thenReturn(Uni.createFrom()
                                                .failure(new RuntimeException("Invalid or expired verification code")));

                VerifyCodeRequest request = VerifyCodeRequest.newBuilder()
                                .setCode("INVALID")
                                .build();

                Auth.ApiResponseVerifyCode response = handler.verifyCode(request).await().indefinitely();

                assertThat(response.getStatus()).isEqualTo("failed");
                assertThat(response.getMessage()).contains("Invalid or expired");
        }
}

