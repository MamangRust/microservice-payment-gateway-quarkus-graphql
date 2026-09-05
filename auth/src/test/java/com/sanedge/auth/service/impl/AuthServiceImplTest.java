package com.sanedge.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.auth.service.AuthService;
import com.sanedge.auth.service.KafkaService;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import pb.user.User.ApiResponseUser;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.user.UserCommand.VerifyPasswordResponse;
import pb.user.UserCommandService;
import pb.user.UserQuery.ApiResponsePaginationUser;
import pb.user.UserQueryService;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

        @Mock
        UserQueryService userQueryService;

        @Mock
        UserCommandService userCommandService;

        @Mock
        RefreshTokenRepository refreshTokenRepository;

        @Mock
        ResetTokenRepository resetTokenRepository;

        @Mock
        RedisService redisService;

        @Mock
        KafkaService kafkaService;

        @Mock
        JwtUtil jwtUtil;

        @Mock
        TracingMetrics tracingMetrics;

        @InjectMocks
        AuthService authServiceUnderTest;

        private RegisterRequest registerReq;
        private UserResponse userResponse;

        @BeforeEach
        void setUp() {
                registerReq = RegisterRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("john@example.com")
                                .password("SecurePass123!")
                                .build();

                userResponse = UserResponse.newBuilder()
                                .setId(1)
                                .setFirstname("John")
                                .setLastname("Doe")
                                .setEmail("john@example.com")
                                .build();

                // Stub both traceAndMeasure overloads so the Supplier is invoked
                // instead of Mockito returning a null Uni.
                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics).traceAndMeasure(anyString(), anyString(), any());
                lenient().doAnswer(invokeSupplier())
                                .when(tracingMetrics)
                                                .traceAndMeasure(anyString(), anyString(), any(Attributes.class), any());

                lenient().when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(false));
                lenient().when(redisService.getReactive(anyString())).thenReturn(Uni.createFrom().nullItem());
                lenient().when(redisService.setWithExpirationReactive(anyString(), anyString(), any(Long.class)))
                                .thenReturn(Uni.createFrom().voidItem());
                lenient().when(redisService.deleteReactive(anyString())).thenReturn(Uni.createFrom().voidItem());

                lenient().when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
                                .thenReturn(Uni.createFrom().voidItem());

                lenient().when(jwtUtil.generateToken(anyString(), any(), any(Long.class)))
                                .thenReturn("access-token-john");
                lenient().when(jwtUtil.generateRefreshToken(anyString(), any(Long.class)))
                                .thenReturn("refresh-token-john");
                lenient().when(jwtUtil.validateToken(anyString())).thenReturn(true);
                lenient().when(jwtUtil.getRefreshExpirationMs()).thenReturn(3600000L);
        }

        @Test
        void registerUser_shouldSucceed() {
                ApiResponsePaginationUser findAllResp = ApiResponsePaginationUser.newBuilder()
                                .setStatus("success")
                                .build();
                when(userQueryService.findAll(any(FindAllUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findAllResp));

                ApiResponseUser createResp = ApiResponseUser.newBuilder()
                                .setStatus("success")
                                .setData(userResponse)
                                .build();
                when(userCommandService.create(any(CreateUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(createResp));

                UserResponse result = authServiceUnderTest.register(registerReq).await().indefinitely();

                assertThat(result).isNotNull();
                assertThat(result.getEmail()).isEqualTo("john@example.com");
                assertThat(result.getFirstname()).isEqualTo("John");
                verify(userQueryService).findAll(any(FindAllUserRequest.class));
                verify(userCommandService).create(any(CreateUserRequest.class));
                verify(kafkaService).sendMessage(anyString(), anyString(), any(JsonObject.class));
        }

        @Test
        void registerUser_shouldFail_whenEmailAlreadyExists() {
                ApiResponsePaginationUser findAllResp = ApiResponsePaginationUser.newBuilder()
                                .setStatus("success")
                                .addData(userResponse)
                                .build();
                when(userQueryService.findAll(any(FindAllUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findAllResp));

                try {
                        authServiceUnderTest.register(registerReq).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("already exists");
                }
        }

        @Test
        void login_shouldSucceed() {
                VerifyPasswordResponse verifyResp = VerifyPasswordResponse.newBuilder()
                                .setValid(true)
                                .setUser(userResponse)
                                .build();
                when(userCommandService.verifyPassword(any(VerifyPasswordRequest.class)))
                                .thenReturn(Uni.createFrom().item(verifyResp));

                lenient().when(refreshTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
                lenient().when(refreshTokenRepository.persist(any(RefreshToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

                String[] tokens = authServiceUnderTest.login("john@example.com", "SecurePass123!").
                                await()
                                .indefinitely();

                assertThat(tokens).hasSize(2);
                assertThat(tokens[0]).isEqualTo("access-token-john");
                assertThat(tokens[1]).isEqualTo("refresh-token-john");
        }

        @Test
        void login_shouldFail_whenAccountLocked() {
                when(redisService.existsReactive(anyString())).thenReturn(Uni.createFrom().item(true));

                try {
                        authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("locked");
                }
        }

        @Test
        void login_shouldFail_withInvalidCredentials() {
                VerifyPasswordResponse verifyResp = VerifyPasswordResponse.newBuilder()
                                .setValid(false)
                                .build();
                when(userCommandService.verifyPassword(any(VerifyPasswordRequest.class)))
                                .thenReturn(Uni.createFrom().item(verifyResp));

                try {
                        authServiceUnderTest.login("john@example.com", "wrong").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid credentials");
                }
        }

        @Test
        void refresh_shouldSucceed() {
                RefreshToken storedToken = new RefreshToken();
                storedToken.setToken("old-refresh-token");
                storedToken.setUserId(1L);
                storedToken.setExpiration(new Timestamp(System.currentTimeMillis() + 3600000));

                when(refreshTokenRepository.findByToken("old-refresh-token"))
                                .thenReturn(Uni.createFrom().item(storedToken));
                when(refreshTokenRepository.persist(any(RefreshToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((RefreshToken) inv.getArgument(0)));

                ApiResponseUser findByIdResp = ApiResponseUser.newBuilder()
                                .setStatus("success")
                                .setData(userResponse)
                                .build();
                when(userQueryService.findById(any(FindByIdUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findByIdResp));

                String[] tokens = authServiceUnderTest.refresh("old-refresh-token").await().indefinitely();

                assertThat(tokens).hasSize(2);
                assertThat(tokens[0]).isEqualTo("access-token-john");
                verify(refreshTokenRepository).persist(any(RefreshToken.class));
        }

        @Test
        void refresh_shouldFail_whenTokenInvalid() {
                when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

                try {
                        authServiceUnderTest.refresh("invalid-token").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid or expired");
                }
        }

        @Test
        void refresh_shouldFail_whenTokenNotFound() {
                when(refreshTokenRepository.findByToken("unknown-token"))
                                .thenReturn(Uni.createFrom().nullItem());

                try {
                        authServiceUnderTest.refresh("unknown-token").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("invalid or expired");
                }
        }

        @Test
        void forgotPassword_shouldSucceed() {
                ApiResponsePaginationUser findAllResp = ApiResponsePaginationUser.newBuilder()
                                .setStatus("success")
                                .addData(userResponse)
                                .build();
                when(userQueryService.findAll(any(FindAllUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findAllResp));

                lenient().when(resetTokenRepository.deleteByUserId(1L)).thenReturn(Uni.createFrom().item(1L));
                lenient().when(resetTokenRepository.persist(any(ResetToken.class)))
                                .thenAnswer(inv -> Uni.createFrom().item((ResetToken) inv.getArgument(0)));

                authServiceUnderTest.forgotPassword("john@example.com").await().indefinitely();

                verify(resetTokenRepository).persist(any(ResetToken.class));
                verify(kafkaService).sendMessage(anyString(), anyString(), any(JsonObject.class));
        }

        @Test
        void forgotPassword_shouldFail_whenUserNotFound() {
                ApiResponsePaginationUser findAllResp = ApiResponsePaginationUser.newBuilder()
                                .setStatus("success")
                                .build();
                when(userQueryService.findAll(any(FindAllUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findAllResp));

                try {
                        authServiceUnderTest.forgotPassword("unknown@example.com").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("User not found");
                }
        }

        @Test
        void resetPassword_shouldSucceed() {
                ResetToken resetToken = new ResetToken();
                resetToken.setToken("valid-reset-token");
                resetToken.setUserId(1L);
                resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000));

                when(resetTokenRepository.findByToken("valid-reset-token"))
                                .thenReturn(Uni.createFrom().item(resetToken));

                ApiResponseUser findByIdResp = ApiResponseUser.newBuilder()
                                .setStatus("success")
                                .setData(userResponse)
                                .build();
                when(userQueryService.findById(any(FindByIdUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findByIdResp));

                ApiResponseUser updateResp = ApiResponseUser.newBuilder()
                                .setStatus("success")
                                .build();
                when(userCommandService.update(any(UpdateUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(updateResp));

                lenient().when(resetTokenRepository.delete(any(ResetToken.class)))
                                .thenReturn(Uni.createFrom().voidItem());

                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("valid-reset-token")
                                .password("NewPass123!")
                                .confirmPassword("NewPass123!")
                                .build();

                authServiceUnderTest.resetPassword(req).await().indefinitely();

                verify(userCommandService).update(any(UpdateUserRequest.class));
        }

        @Test
        void resetPassword_shouldFail_whenPasswordsMismatch() {
                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("some-token")
                                .password("Pass1!")
                                .confirmPassword("Pass2!")
                                .build();

                try {
                        authServiceUnderTest.resetPassword(req).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("do not match");
                }
        }

        @Test
        void resetPassword_shouldFail_whenTokenExpired() {
                ResetToken expiredToken = new ResetToken();
                expiredToken.setToken("expired-token");
                expiredToken.setUserId(1L);
                expiredToken.setExpiration(new Timestamp(System.currentTimeMillis() - 3600000));

                when(resetTokenRepository.findByToken("expired-token"))
                                .thenReturn(Uni.createFrom().item(expiredToken));

                ResetPasswordRequest req = ResetPasswordRequest.builder()
                                .token("expired-token")
                                .password("NewPass123!")
                                .confirmPassword("NewPass123!")
                                .build();

                try {
                        authServiceUnderTest.resetPassword(req).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("expired");
                }
        }

        @Test
        void logout_shouldSucceed() {
                when(refreshTokenRepository.deleteByToken("refresh-token-to-revoke"))
                                .thenReturn(Uni.createFrom().item(1L));

                authServiceUnderTest.logout("refresh-token-to-revoke").await().indefinitely();

                verify(refreshTokenRepository).deleteByToken("refresh-token-to-revoke");
        }

        @Test
        void getMe_shouldReturnUser() {
                ApiResponseUser findByIdResp = ApiResponseUser.newBuilder()
                                .setStatus("success")
                                .setData(userResponse)
                                .build();
                when(userQueryService.findById(any(FindByIdUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findByIdResp));

                UserResponse result = authServiceUnderTest.getMe(1L).await().indefinitely();

                assertThat(result).isNotNull();
                assertThat(result.getId()).isEqualTo(1);
                assertThat(result.getEmail()).isEqualTo("john@example.com");
        }

        @Test
        void getMe_shouldFail_whenUserNotFound() {
                ApiResponseUser findByIdResp = ApiResponseUser.newBuilder()
                                .setStatus("error")
                                .build();
                when(userQueryService.findById(any(FindByIdUserRequest.class)))
                                .thenReturn(Uni.createFrom().item(findByIdResp));

                try {
                        authServiceUnderTest.getMe(999L).await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("User not found");
                }
        }

        @Test
        void verifyEmail_shouldSucceed() {
                when(redisService.getReactive("verification_code:ABC123"))
                                .thenReturn(Uni.createFrom().item("john@example.com"));

                authServiceUnderTest.verifyEmailByCode("ABC123").await().indefinitely();

                verify(redisService).deleteReactive("verification_code:ABC123");
        }

        @Test
        void verifyEmail_shouldFail_whenInvalidCode() {
                when(redisService.getReactive("verification_code:INVALID"))
                                .thenReturn(Uni.createFrom().nullItem());

                try {
                        authServiceUnderTest.verifyEmailByCode("INVALID").await().indefinitely();
                } catch (RuntimeException e) {
                        assertThat(e.getMessage()).contains("Invalid or expired");
                }
        }

        /**
         * Finds the Supplier argument in the invocation regardless of whether it was
         * passed positionally in the 3-arg overload (arg index 2) or 4-arg overload
         * (arg index 3), then invokes it and returns the resulting Uni. This lets
         * a single Answer<?> body serve both traceAndMeasure overloads.
         */
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
