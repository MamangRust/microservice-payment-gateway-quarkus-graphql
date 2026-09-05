package com.sanedge.auth.handler;

import com.sanedge.auth.service.AuthService;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

import pb.Auth.ApiResponseVerifyCode;
import pb.Auth.VerifyCodeRequest;
import pb.Auth.ApiResponseForgotPassword;
import pb.Auth.ForgotPasswordRequest;
import pb.Auth.ApiResponseResetPassword;
import pb.Auth.ApiResponseRegister;
import pb.Auth.ApiResponseLogin;
import pb.Auth.LoginRequest;
import pb.Auth.TokenResponse;
import pb.Auth.ApiResponseRefreshToken;
import pb.Auth.RefreshTokenRequest;
import pb.Auth.ApiResponseGetMe;
import pb.Auth.GetMeRequest;

@GrpcService
public class AuthGrpcHandler extends pb.MutinyAuthServiceGrpc.AuthServiceImplBase {

    @Inject
    AuthService authService;

    @Override
    public Uni<ApiResponseVerifyCode> verifyCode(VerifyCodeRequest request) {
        return authService.verifyEmailByCode(request.getCode())
                .map(v -> ApiResponseVerifyCode.newBuilder()
                        .setStatus("success")
                        .setMessage("Email verified successfully")
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseVerifyCode.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseForgotPassword> forgotPassword(ForgotPasswordRequest request) {
        return authService.forgotPassword(request.getEmail())
                .map(v -> ApiResponseForgotPassword.newBuilder()
                        .setStatus("success")
                        .setMessage("Password reset email sent successfully")
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseForgotPassword.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseResetPassword> resetPassword(pb.Auth.ResetPasswordRequest request) {
        com.sanedge.auth.domain.requests.ResetPasswordRequest domainReq = com.sanedge.auth.domain.requests.ResetPasswordRequest.builder()
                .token(request.getResetToken())
                .password(request.getPassword())
                .confirmPassword(request.getConfirmPassword())
                .build();

        return authService.resetPassword(domainReq)
                .map(v -> ApiResponseResetPassword.newBuilder()
                        .setStatus("success")
                        .setMessage("Password reset successfully")
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseResetPassword.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseRegister> registerUser(pb.Auth.RegisterRequest request) {
        com.sanedge.auth.domain.requests.RegisterRequest domainReq = com.sanedge.auth.domain.requests.RegisterRequest.builder()
                .firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                .password(request.getPassword())
                .build();

        return authService
                .register(domainReq)
                .map(user -> ApiResponseRegister.newBuilder()
                        .setStatus("success")
                        .setMessage("User registered successfully. Verification email sent.")
                        .setData(user)
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseRegister.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseLogin> loginUser(LoginRequest request) {
        return authService.login(request.getEmail(), request.getPassword())
                .map(tokens -> ApiResponseLogin.newBuilder()
                        .setStatus("success")
                        .setMessage("Logged in successfully")
                        .setData(TokenResponse.newBuilder()
                                .setAccessToken(tokens[0])
                                .setRefreshToken(tokens[1])
                                .build())
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseLogin.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseRefreshToken> refreshToken(RefreshTokenRequest request) {
        return authService.refresh(request.getRefreshToken())
                .map(tokens -> ApiResponseRefreshToken.newBuilder()
                        .setStatus("success")
                        .setMessage("Token refreshed successfully")
                        .setData(TokenResponse.newBuilder()
                                .setAccessToken(tokens[0])
                                .setRefreshToken(tokens[1])
                                .build())
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseRefreshToken.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }

    @Override
    public Uni<ApiResponseGetMe> getMe(GetMeRequest request) {
        return authService.getMe((long) request.getUserId())
                .map(user -> ApiResponseGetMe.newBuilder()
                        .setStatus("success")
                        .setMessage("Profile retrieved successfully")
                        .setData(user)
                        .build())
                .onFailure().recoverWithItem(err -> ApiResponseGetMe.newBuilder()
                        .setStatus("failed")
                        .setMessage(err.getMessage())
                        .build());
    }
}
