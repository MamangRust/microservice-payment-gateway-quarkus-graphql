package com.sanedge.auth.service;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.auth.repository.RefreshTokenRepository;
import com.sanedge.auth.repository.ResetTokenRepository;
import com.sanedge.auth.domain.requests.RegisterRequest;
import com.sanedge.auth.domain.requests.ResetPasswordRequest;
import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.role.UserRolePort;
import com.sanedge.common.adapter.user.AuthUserPort;
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.core.json.JsonObject;
import pb.user.User.UserResponse;

@ApplicationScoped
public class AuthService {

    @Inject
    AuthUserPort authUserPort;

    @Inject
    UserRolePort userRolePort;

    @Inject
    RefreshTokenRepository refreshTokenRepository;

    @Inject
    ResetTokenRepository resetTokenRepository;

    @Inject
    RedisService redisService;

    @Inject
    KafkaService kafkaService;

    @Inject
    JwtUtil jwtUtil;

    @Inject
    PasswordUtil passwordUtil;

    @Inject
    TracingMetrics tracingMetrics;

    @WithTransaction
    public Uni<UserResponse> register(RegisterRequest req) {
        String firstName = req.getFirstName();
        String lastName = req.getLastName();
        String email = req.getEmail();
        String password = req.getPassword();

        return tracingMetrics.traceAndMeasure("registerUser", "register", () -> {
            return authUserPort.findByEmail(email)
                    .chain(existing -> {
                        if (existing.isPresent()) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("User with this email already exists"));
                        }
                        // User service assigns the default ROLE_USER during creation.
                        return authUserPort.create(new AuthUserPort.RegisterData(
                                firstName, lastName, email, password, password));
                    })
                    .chain(user -> {
                        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                        return redisService.setWithExpirationReactive("verification:" + email, verificationCode, 900)
                                .chain(() -> redisService.setWithExpirationReactive(
                                        "verification_code:" + verificationCode,
                                        email, 900))
                                .invoke(() -> sendWelcomeEmail(user, verificationCode)
                                        .onFailure().recoverWithNull())
                                .replaceWith(toProtoUser(user));
                    });
        });
    }

    @WithTransaction
    public Uni<String[]> login(String email, String password) {
        String failedAttemptsKey = "failed_login:" + email;
        String lockKey = "account_locked:" + email;

        return tracingMetrics.traceAndMeasure("loginUser", "login", () -> {
            return redisService.existsReactive(lockKey)
                    .chain(locked -> {
                        if (locked) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Account is locked due to too many failed attempts"));
                        }
                        return authUserPort.verifyPassword(email, password);
                    })
                    .chain(verifyRes -> {
                        if (!verifyRes.valid()) {
                            return handleFailedLogin(email, failedAttemptsKey, lockKey);
                        }

                        User user = verifyRes.user();

                        return rolesForUser(user.id())
                                .chain(roles -> {
                                    String accessToken = jwtUtil.generateToken(user.email(), roles,
                                            (long) user.id());
                                    String refreshTokenStr = jwtUtil.generateRefreshToken(user.email(),
                                            (long) user.id());

                                    RefreshToken rt = new RefreshToken();
                                    rt.setUserId((long) user.id());
                                    rt.setToken(refreshTokenStr);
                                    rt.setExpiration(new Timestamp(System.currentTimeMillis()
                                            + jwtUtil.getRefreshExpirationMs()));

                                    return redisService.deleteReactive(failedAttemptsKey)
                                            .chain(() -> refreshTokenRepository.deleteByUserId((long) user.id()))
                                            .chain(() -> refreshTokenRepository.persist(rt))
                                            .map(v -> new String[] { accessToken, refreshTokenStr });
                                });
                    });
        });
    }

    @WithTransaction
    public Uni<String[]> refresh(String refreshTokenStr) {
        return tracingMetrics.traceAndMeasure("refreshToken", "refresh", () -> {
            if (!jwtUtil.validateToken(refreshTokenStr)) {
                return Uni.createFrom().failure(new RuntimeException("Invalid or expired refresh token"));
            }

            return refreshTokenRepository.findByToken(refreshTokenStr)
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Refresh token is invalid or expired"));
                        }

                        return authUserPort.findById(rt.getUserId().intValue())
                                .chain(user -> rolesForUser(user.id())
                                        .map(roles -> {
                                            String newAccessToken = jwtUtil.generateToken(user.email(), roles,
                                                    (long) user.id());
                                            String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.email(),
                                                    (long) user.id());

                                            rt.setToken(newRefreshTokenStr);
                                            rt.setExpiration(new Timestamp(System.currentTimeMillis()
                                                    + jwtUtil.getRefreshExpirationMs()));
                                            return new String[] { newAccessToken, newRefreshTokenStr };
                                        })
                                        .call(() -> refreshTokenRepository.persist(rt)));
                    });
        });
    }

    @WithTransaction
    public Uni<Void> forgotPassword(String email) {
        return tracingMetrics.traceAndMeasure("forgotPassword", "forgot_password", () -> {
            return authUserPort.findByEmail(email)
                    .chain(found -> {
                        if (found.isEmpty()) {
                            return Uni.createFrom().failure(new RuntimeException("User not found"));
                        }

                        User user = found.get();
                        String token = UUID.randomUUID().toString();

                        ResetToken resetToken = new ResetToken();
                        resetToken.setUserId((long) user.id());
                        resetToken.setToken(token);
                        resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000)); // 15 mins

                        return resetTokenRepository.deleteByUserId((long) user.id())
                                .chain(() -> resetTokenRepository.persist(resetToken))
                                .chain(() -> sendForgotPasswordEmail(user, token));
                    });
        });
    }

    @WithTransaction
    public Uni<Void> resetPassword(ResetPasswordRequest req) {
        String token = req.getToken();
        String password = req.getPassword();
        String confirmPassword = req.getConfirmPassword();

        return tracingMetrics.traceAndMeasure("resetPassword", "reset_password", () -> {
            if (!password.equals(confirmPassword)) {
                return Uni.createFrom().failure(new RuntimeException("Passwords do not match"));
            }

            return resetTokenRepository.findByToken(token)
                    .chain(rt -> {
                        if (rt == null || rt.getExpiration().before(new Timestamp(System.currentTimeMillis()))) {
                            return Uni.createFrom().failure(new RuntimeException("Invalid or expired reset token"));
                        }

                        return authUserPort.findById(rt.getUserId().intValue())
                                .chain(user -> authUserPort.update(new AuthUserPort.UpdateData(
                                        user.id(), user.firstname(), user.lastname(), user.email(),
                                        password, confirmPassword)))
                                .chain(updated -> resetTokenRepository.delete(rt))
                                .replaceWithVoid();
                    });
        });
    }

    @WithTransaction
    public Uni<Void> logout(String refreshTokenStr) {
        return tracingMetrics.traceAndMeasure("logout", "logout",
                () -> refreshTokenRepository.deleteByToken(refreshTokenStr)
                        .replaceWithVoid());
    }

    public Uni<Void> verifyEmailByCode(String code) {
        return tracingMetrics.traceAndMeasure("verifyEmailByCode", "verify_email", () -> {
            String key = "verification_code:" + code;
            return redisService.getReactive(key)
                    .chain(email -> {
                        if (email == null) {
                            return Uni.createFrom()
                                    .failure(new RuntimeException("Invalid or expired verification code"));
                        }
                        return redisService.deleteReactive(key)
                                .chain(() -> redisService.deleteReactive("verification:" + email))
                                .replaceWithVoid();
                    });
        });
    }

    public Uni<UserResponse> getMe(Long userId) {
        return tracingMetrics.traceAndMeasure("getMe", "get_me",
                () -> authUserPort.findById(userId.intValue()).map(AuthService::toProtoUser));
    }

    private Uni<List<String>> rolesForUser(int userId) {
        return userRolePort.findByUserId(userId)
                .map(roles -> roles.stream()
                        .map(Role::name)
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.toList()))
                .map(roles -> roles.isEmpty()
                        ? Collections.singletonList("ROLE_USER")
                        : roles)
                .onFailure().recoverWithItem(Collections.singletonList("ROLE_USER"));
    }

    private Uni<String[]> handleFailedLogin(String email, String failedAttemptsKey, String lockKey) {
        return redisService.getReactive(failedAttemptsKey)
                .chain(attemptsStr -> {
                    int currentAttempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);
                    int newAttempts = currentAttempts + 1;
                    if (newAttempts >= 5) {
                        return redisService.setWithExpirationReactive(lockKey, "true", 3600) // lock 1 hr
                                .chain(() -> redisService.deleteReactive(failedAttemptsKey))
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Account is locked due to too many failed attempts")));
                    } else {
                        return redisService
                                .setWithExpirationReactive(failedAttemptsKey, String.valueOf(newAttempts), 600) // 10
                                                                                                                // mins
                                .chain(() -> Uni.createFrom().failure(
                                        new RuntimeException("Invalid credentials. Attempt " + newAttempts + " of 5")));
                    }
                });
    }

    private Uni<Void> sendWelcomeEmail(User user, String code) {
        String subject = "Welcome to Quarkus Modular Monolith";
        String body = String.format(
                "Hello %s %s,\n\nWelcome to our platform! Use the following code to verify your email address:\n\n%s\n\nRegards,\nSupport Team",
                user.firstname(), user.lastname(), code);

        JsonObject payload = new JsonObject()
                .put("email", user.email())
                .put("subject", subject)
                .put("body", body);

        return kafkaService.sendMessage("email-service-topic-auth-register", user.email(), payload);
    }

    private Uni<Void> sendForgotPasswordEmail(User user, String token) {
        String subject = "Reset Password Verification";
        String body = String.format(
                "Hello %s %s,\n\nYou have requested a password reset. Use the following token to reset your password:\n\n%s\n\nThis token will expire in 15 minutes.\n\nRegards,\nSupport Team",
                user.firstname(), user.lastname(), token);

        JsonObject payload = new JsonObject()
                .put("email", user.email())
                .put("subject", subject)
                .put("body", body);

        return kafkaService.sendMessage("email-service-topic-auth-forgot-password", user.email(), payload);
    }

    private static UserResponse toProtoUser(User u) {
        if (u == null) {
            return UserResponse.getDefaultInstance();
        }
        return UserResponse.newBuilder()
                .setId(u.id())
                .setFirstname(nullSafe(u.firstname()))
                .setLastname(nullSafe(u.lastname()))
                .setEmail(nullSafe(u.email()))
                .setCreatedAt(u.createdAt() == null ? "" : DateTimeFormatter.ISO_INSTANT.format(u.createdAt()))
                .setUpdatedAt(u.updatedAt() == null ? "" : DateTimeFormatter.ISO_INSTANT.format(u.updatedAt()))
                .build();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
