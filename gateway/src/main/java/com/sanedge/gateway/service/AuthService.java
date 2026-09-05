package com.sanedge.gateway.service;

import com.sanedge.gateway.dto.AuthDto.*;
import io.smallrye.mutiny.Uni;

public interface AuthService {
    Uni<RegisterResponse> register(RegisterRequest body);
    Uni<LoginResponse> login(LoginRequest body);
    Uni<SimpleResponse> verify(VerifyCodeRequest body);
    Uni<SimpleResponse> forgotPassword(ForgotPasswordRequest body);
    Uni<SimpleResponse> resetPassword(ResetPasswordRequest body);
    Uni<RefreshTokenResponse> refresh(RefreshTokenRequest body);
    Uni<GetMeResponse> getMe(int userId);
}
