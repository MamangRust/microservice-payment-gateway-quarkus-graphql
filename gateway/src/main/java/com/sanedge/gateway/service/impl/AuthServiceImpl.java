package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.AuthDto.*;
import com.sanedge.gateway.service.AuthService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AuthServiceImpl implements AuthService {

    private static final Logger LOG = Logger.getLogger(AuthServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("auth")
    pb.MutinyAuthServiceGrpc.MutinyAuthServiceStub authService;

    @Override
    public Uni<RegisterResponse> register(RegisterRequest body) {
        return telemetryHelper.traceAndMetric("auth.register", () -> authService.registerUser(pb.Auth.RegisterRequest.newBuilder()
                .setFirstname(body.firstname() == null ? "" : body.firstname())
                .setLastname(body.lastname() == null ? "" : body.lastname())
                .setEmail(body.email() == null ? "" : body.email())
                .setPassword(body.password() == null ? "" : body.password())
                .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                .build())
                .map(RegisterResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to register: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<LoginResponse> login(LoginRequest body) {
        return telemetryHelper.traceAndMetric("auth.login", () -> authService.loginUser(pb.Auth.LoginRequest.newBuilder()
                .setEmail(body.email() == null ? "" : body.email())
                .setPassword(body.password() == null ? "" : body.password())
                .build())
                .map(LoginResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to login: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleResponse> verify(VerifyCodeRequest body) {
        return telemetryHelper.traceAndMetric("auth.verify", () -> authService.verifyCode(pb.Auth.VerifyCodeRequest.newBuilder()
                .setCode(body.code() == null ? "" : body.code())
                .build())
                .map(SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to verify code: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleResponse> forgotPassword(ForgotPasswordRequest body) {
        return telemetryHelper.traceAndMetric("auth.forgotPassword", () -> authService.forgotPassword(pb.Auth.ForgotPasswordRequest.newBuilder()
                .setEmail(body.email() == null ? "" : body.email())
                .build())
                .map(SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed forgot password: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<SimpleResponse> resetPassword(ResetPasswordRequest body) {
        return telemetryHelper.traceAndMetric("auth.resetPassword", () -> authService.resetPassword(pb.Auth.ResetPasswordRequest.newBuilder()
                .setResetToken(body.resetToken() == null ? "" : body.resetToken())
                .setPassword(body.password() == null ? "" : body.password())
                .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                .build())
                .map(SimpleResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to reset password: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<RefreshTokenResponse> refresh(RefreshTokenRequest body) {
        return telemetryHelper.traceAndMetric("auth.refresh", () -> authService.refreshToken(pb.Auth.RefreshTokenRequest.newBuilder()
                .setRefreshToken(body.refreshToken() == null ? "" : body.refreshToken())
                .build())
                .map(RefreshTokenResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to refresh token: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<GetMeResponse> getMe(int userId) {
        return telemetryHelper.traceAndMetric("auth.getMe", () -> authService.getMe(pb.Auth.GetMeRequest.newBuilder()
                .setUserId(userId)
                .build())
                .map(GetMeResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get profile: " + throwable.getMessage(), throwable)));
    }
}
