package com.sanedge.gateway.dto;

import java.util.List;

public class UserDto {

        @org.eclipse.microprofile.graphql.Name("UserResponse")
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
            public static UserResponse from(pb.user.User.UserResponseDeleteAt proto) {
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

        public record FindAllUserResponse(
                List<UserResponse> data,
                String status,
                String message) {
            public static FindAllUserResponse from(pb.user.UserQuery.ApiResponsePaginationUser proto) {
                return new FindAllUserResponse(
                        proto.getDataList().stream().map(UserResponse::from).toList(),
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record FindByIdUserResponse(
                UserResponse data,
                String status,
                String message) {
            public static FindByIdUserResponse from(pb.user.User.ApiResponseUser proto) {
                return new FindByIdUserResponse(
                        proto.hasData() ? UserResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record CreateUserRequest(
                String firstname,
                String lastname,
                String email,
                String password,
                String confirmPassword) {}

        public record CreateUserResponse(
                UserResponse data,
                String status,
                String message) {
            public static CreateUserResponse from(pb.user.User.ApiResponseUser proto) {
                return new CreateUserResponse(
                        proto.hasData() ? UserResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record UpdateUserRequest(
                String firstname,
                String lastname,
                String email,
                String password,
                String confirmPassword) {}

        public record UpdateUserResponse(
                UserResponse data,
                String status,
                String message) {
            public static UpdateUserResponse from(pb.user.User.ApiResponseUser proto) {
                return new UpdateUserResponse(
                        proto.hasData() ? UserResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }

        public record TrashedUserResponse(
                UserResponse data,
                String status,
                String message) {
            public static TrashedUserResponse from(pb.user.User.ApiResponseUserDeleteAt proto) {
                return new TrashedUserResponse(
                        proto.hasData() ? UserResponse.from(proto.getData()) : null,
                        proto.getStatus(),
                        proto.getMessage()
                );
            }
        }
}
