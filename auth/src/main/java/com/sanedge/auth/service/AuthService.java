package com.sanedge.auth.service;

import java.sql.Timestamp;
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
import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.utils.JwtUtil;
import com.sanedge.common.utils.PasswordUtil;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.core.json.JsonObject;
import pb.user.UserQueryService;
import pb.user.UserCommandService;
import pb.user.User.UserResponse;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.RoleService;

@ApplicationScoped
public class AuthService {

    @GrpcClient("user")
    UserQueryService userQueryService;

    @GrpcClient("user")
    UserCommandService userCommandService;

    @GrpcClient("role")
    RoleService roleService;

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
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() > 0) {
                            for (UserResponse u : findAllResponse.getDataList()) {
                                if (u.getEmail().equalsIgnoreCase(email)) {
                                    return Uni.createFrom()
                                            .failure(new RuntimeException("User with this email already exists"));
                                }
                            }
                        }

                        CreateUserRequest createReq = CreateUserRequest.newBuilder()
                                .setFirstname(firstName)
                                .setLastname(lastName)
                                .setEmail(email)
                                .setPassword(password)
                                .setConfirmPassword(password)
                                .build();

                        return userCommandService.create(createReq);
                    })
                    .chain(createUserResponse -> {
                        if (!"success".equalsIgnoreCase(createUserResponse.getStatus())) {
                            return Uni.createFrom().failure(new RuntimeException(createUserResponse.getMessage()));
                        }

                        UserResponse user = createUserResponse.getData();
                        String verificationCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();

                        return redisService.setWithExpirationReactive("verification:" + email, verificationCode, 900)
                                .chain(() -> redisService.setWithExpirationReactive(
                                        "verification_code:" + verificationCode,
                                        email, 900))
                                .invoke(() -> sendWelcomeEmail(user, verificationCode)
                                        .onFailure().recoverWithNull())
                                // User service assigns the default ROLE_USER during creation.
                                .replaceWith(user);
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
                        return userCommandService.verifyPassword(VerifyPasswordRequest.newBuilder()
                                .setEmail(email)
                                .setPassword(password)
                                .build());
                    })
                    .chain(verifyRes -> {
                        if (!verifyRes.getValid()) {
                            return handleFailedLogin(email, failedAttemptsKey, lockKey);
                        }

                        UserResponse user = verifyRes.getUser();

                        return rolesForUser(user.getId())
                                .chain(roles -> {
                                    String accessToken = jwtUtil.generateToken(user.getEmail(), roles,
                                            (long) user.getId());
                                    String refreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(),
                                            (long) user.getId());

                                    RefreshToken rt = new RefreshToken();
                                    rt.setUserId((long) user.getId());
                                    rt.setToken(refreshTokenStr);
                                    rt.setExpiration(new Timestamp(System.currentTimeMillis()
                                            + jwtUtil.getRefreshExpirationMs()));

                                    return redisService.deleteReactive(failedAttemptsKey)
                                            .chain(() -> refreshTokenRepository.deleteByUserId((long) user.getId()))
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

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    return rolesForUser(user.getId())
                                            .map(roles -> {
                                                String newAccessToken = jwtUtil.generateToken(user.getEmail(), roles,
                                                        (long) user.getId());
                                                String newRefreshTokenStr = jwtUtil.generateRefreshToken(user.getEmail(),
                                                        (long) user.getId());

                                                rt.setToken(newRefreshTokenStr);
                                                rt.setExpiration(new Timestamp(System.currentTimeMillis()
                                                        + jwtUtil.getRefreshExpirationMs()));
                                                return new String[] { newAccessToken, newRefreshTokenStr };
                                            })
                                            .call(() -> refreshTokenRepository.persist(rt));
                                });
                    });
        });
    }

    @WithTransaction
    public Uni<Void> forgotPassword(String email) {
        return tracingMetrics.traceAndMeasure("forgotPassword", "forgot_password", () -> {
            return userQueryService
                    .findAll(FindAllUserRequest.newBuilder().setSearch(email).setPage(1).setPageSize(1).build())
                    .chain(findAllResponse -> {
                        if (findAllResponse.getDataCount() == 0) {
                            return Uni.createFrom().failure(new RuntimeException("User not found"));
                        }

                        UserResponse user = findAllResponse.getData(0);
                        String token = UUID.randomUUID().toString();

                        ResetToken resetToken = new ResetToken();
                        resetToken.setUserId((long) user.getId());
                        resetToken.setToken(token);
                        resetToken.setExpiration(new Timestamp(System.currentTimeMillis() + 900000)); // 15 mins

                        return resetTokenRepository.deleteByUserId((long) user.getId())
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

                        return userQueryService
                                .findById(FindByIdUserRequest.newBuilder().setId(rt.getUserId().intValue()).build())
                                .chain(userRes -> {
                                    if (!"success".equalsIgnoreCase(userRes.getStatus()) || !userRes.hasData()) {
                                        return Uni.createFrom().failure(new RuntimeException("User not found"));
                                    }

                                    UserResponse user = userRes.getData();
                                    UpdateUserRequest updateReq = UpdateUserRequest.newBuilder()
                                            .setId(user.getId())
                                            .setFirstname(user.getFirstname())
                                            .setLastname(user.getLastname())
                                            .setEmail(user.getEmail())
                                            .setPassword(password)
                                            .setConfirmPassword(confirmPassword)
                                            .build();

                                    return userCommandService.update(updateReq);
                                })
                                .chain(updateRes -> {
                                    if (!"success".equalsIgnoreCase(updateRes.getStatus())) {
                                        return Uni.createFrom().failure(new RuntimeException(updateRes.getMessage()));
                                    }
                                    return resetTokenRepository.delete(rt);
                                })
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
                () -> userQueryService.findById(FindByIdUserRequest.newBuilder().setId(userId.intValue()).build())
                        .map(res -> {
                            if (!"success".equalsIgnoreCase(res.getStatus()) || !res.hasData()) {
                                throw new RuntimeException("User not found");
                            }
                            return res.getData();
                        }));
    }

    private Uni<List<String>> rolesForUser(int userId) {
        if (roleService == null) {
            return Uni.createFrom().item(Collections.singletonList("ROLE_USER"));
        }

        return roleService.findByUserId(FindByIdUserRoleRequest.newBuilder().setUserId(userId).build())
                .map(response -> response.getDataList().stream()
                        .map(pb.role.Role.RoleResponse::getName)
                        .filter(name -> name != null && !name.isBlank())
                        .collect(Collectors.toList()))
                .onItem().transform(roles -> roles.isEmpty()
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

    private Uni<Void> sendWelcomeEmail(UserResponse user, String code) {
        String subject = "Welcome to Quarkus Modular Monolith";
        String body = String.format(
                "Hello %s %s,\n\nWelcome to our platform! Use the following code to verify your email address:\n\n%s\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), code);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return kafkaService.sendMessage("email-service-topic-auth-register", user.getEmail(), payload);
    }

    private Uni<Void> sendForgotPasswordEmail(UserResponse user, String token) {
        String subject = "Reset Password Verification";
        String body = String.format(
                "Hello %s %s,\n\nYou have requested a password reset. Use the following token to reset your password:\n\n%s\n\nThis token will expire in 15 minutes.\n\nRegards,\nSupport Team",
                user.getFirstname(), user.getLastname(), token);

        JsonObject payload = new JsonObject()
                .put("email", user.getEmail())
                .put("subject", subject)
                .put("body", body);

        return kafkaService.sendMessage("email-service-topic-auth-forgot-password", user.getEmail(), payload);
    }
}