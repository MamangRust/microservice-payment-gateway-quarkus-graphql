package com.sanedge.gateway.resource;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;

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
import com.sanedge.gateway.exception.GraphQLErrorHandler;
import com.sanedge.gateway.service.AuthService;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@GraphQLApi
@Singleton
@GraphQLErrorHandler
public class AuthResource {

    @Inject
    AuthService authService;

    @Mutation("register")
    @Description("Register a new user")
    public Uni<RegisterResponse> register(@Name("body") RegisterRequest body) {
        return authService.register(body);
    }

    @Mutation("login")
    @Description("Login a user")
    public Uni<LoginResponse> login(@Name("body") LoginRequest body) {
        return authService.login(body);
    }

    @Mutation("verify")
    @Description("Verify user email by verification code")
    public Uni<SimpleResponse> verify(@Name("body") VerifyCodeRequest body) {
        return authService.verify(body);
    }

    @Mutation("forgotPassword")
    @Description("Initiate forgot password request")
    public Uni<SimpleResponse> forgotPassword(@Name("body") ForgotPasswordRequest body) {
        return authService.forgotPassword(body);
    }

    @Mutation("resetPassword")
    @Description("Reset user password")
    public Uni<SimpleResponse> resetPassword(@Name("body") ResetPasswordRequest body) {
        return authService.resetPassword(body);
    }

    @Mutation("refresh")
    @Description("Refresh user access token")
    public Uni<RefreshTokenResponse> refresh(@Name("body") RefreshTokenRequest body) {
        return authService.refresh(body);
    }

    @Query("getMe")
    @Description("Get current logged-in user profile")
    @RolesAllowed({"ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"})
    public Uni<GetMeResponse> getMe(@Name("userId") int userId) {
        return authService.getMe(userId);
    }
}
