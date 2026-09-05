package com.sanedge.gateway.dto;

public class AuthDto {
    public record RegisterRequest(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {}

    public record LoginRequest(
            String email,
            String password) {}

    public record VerifyCodeRequest(String code) {}

    public record ForgotPasswordRequest(String email) {}

    public record ResetPasswordRequest(
            String resetToken,
            String password,
            String confirmPassword) {}

    public record RefreshTokenRequest(String refreshToken) {}

    public record GetMeRequest(int userId) {}

    public record TokenResponse(
            String accessToken,
            String refreshToken) {
        public static TokenResponse from(pb.Auth.TokenResponse proto) {
            return new TokenResponse(proto.getAccessToken(), proto.getRefreshToken());
        }
    }

    @org.eclipse.microprofile.graphql.Name("AuthUserResponse")
    public record UserResponse(
            int id,
            String firstname,
            String lastname,
            String email,
            String createdAt,
            String updatedAt) {
        public static UserResponse from(pb.user.User.UserResponse proto) {
            return new UserResponse(
                    proto.getId(),
                    proto.getFirstname(),
                    proto.getLastname(),
                    proto.getEmail(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record RegisterResponse(
            String status,
            String message,
            UserResponse data) {
        public static RegisterResponse from(pb.Auth.ApiResponseRegister proto) {
            return new RegisterResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? UserResponse.from(proto.getData()) : null
            );
        }
    }

    public record LoginResponse(
            String status,
            String message,
            TokenResponse data) {
        public static LoginResponse from(pb.Auth.ApiResponseLogin proto) {
            return new LoginResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? TokenResponse.from(proto.getData()) : null
            );
        }
    }

    public record RefreshTokenResponse(
            String status,
            String message,
            TokenResponse data) {
        public static RefreshTokenResponse from(pb.Auth.ApiResponseRefreshToken proto) {
            return new RefreshTokenResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? TokenResponse.from(proto.getData()) : null
            );
        }
    }

    public record GetMeResponse(
            String status,
            String message,
            UserResponse data) {
        public static GetMeResponse from(pb.Auth.ApiResponseGetMe proto) {
            return new GetMeResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? UserResponse.from(proto.getData()) : null
            );
        }
    }

    public record SimpleResponse(
            String status,
            String message) {
        public static SimpleResponse from(pb.Auth.ApiResponseVerifyCode proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleResponse from(pb.Auth.ApiResponseForgotPassword proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }

        public static SimpleResponse from(pb.Auth.ApiResponseResetPassword proto) {
            return new SimpleResponse(proto.getStatus(), proto.getMessage());
        }
    }
}
