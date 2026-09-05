package com.sanedge.gateway.dto;

import java.util.List;

public class RoleDto {

    public record RoleResponse(
            int id,
            String name,
            String createdAt,
            String updatedAt) {
        public static RoleResponse from(pb.role.Role.RoleResponse proto) {
            return new RoleResponse(proto.getId(), proto.getName(), proto.getCreatedAt(), proto.getUpdatedAt());
        }
        public static RoleResponse from(pb.role.Role.RoleResponseDeleteAt proto) {
            return new RoleResponse(proto.getId(), proto.getName(), proto.getCreatedAt(), proto.getUpdatedAt());
        }
    }

    public record FindAllRoleResponse(
            List<RoleResponse> data,
            String status,
            String message) {
        public static FindAllRoleResponse from(pb.role.RoleQuery.ApiResponsePaginationRole proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllRoleResponse from(pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
        public static FindAllRoleResponse from(pb.role.Role.ApiResponsesRole proto) {
            return new FindAllRoleResponse(
                    proto.getDataList().stream().map(RoleResponse::from).toList(),
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record FindByIdRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static FindByIdRoleResponse from(pb.role.Role.ApiResponseRole proto) {
            return new FindByIdRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record CreateRoleRequest(
            String name) {}

    public record CreateRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static CreateRoleResponse from(pb.role.Role.ApiResponseRole proto) {
            return new CreateRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record UpdateRoleRequest(
            String name) {}

    public record UpdateRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static UpdateRoleResponse from(pb.role.Role.ApiResponseRole proto) {
            return new UpdateRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    public record TrashedRoleResponse(
            RoleResponse data,
            String status,
            String message) {
        public static TrashedRoleResponse from(pb.role.Role.ApiResponseRoleDeleteAt proto) {
            return new TrashedRoleResponse(
                    proto.hasData() ? RoleResponse.from(proto.getData()) : null,
                    proto.getStatus(),
                    proto.getMessage()
            );
        }
    }

    @org.eclipse.microprofile.graphql.Name("RoleSimpleStatusMessageResponse")
    public record SimpleStatusMessageResponse(
            String status,
            String message) {
        public static SimpleStatusMessageResponse from(pb.role.RoleCommand.ApiResponseRoleDelete proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse from(pb.role.RoleCommand.ApiResponseRoleAll proto) {
            return new SimpleStatusMessageResponse(proto.getStatus(), proto.getMessage());
        }
        public static SimpleStatusMessageResponse success(String message) {
            return new SimpleStatusMessageResponse("SUCCESS", message);
        }
    }

    public record UserRoleResponse(
            int userRoleId,
            int userId,
            int roleId,
            String createdAt,
            String updatedAt) {
        public static UserRoleResponse from(pb.role.RoleCommand.UserRoleResponse proto) {
            return new UserRoleResponse(
                    proto.getUserRoleId(),
                    proto.getUserId(),
                    proto.getRoleId(),
                    proto.getCreatedAt(),
                    proto.getUpdatedAt()
            );
        }
    }

    public record AssignRoleToUserResponse(
            String status,
            String message,
            UserRoleResponse data) {
        public static AssignRoleToUserResponse from(pb.role.RoleCommand.ApiResponseUserRole proto) {
            return new AssignRoleToUserResponse(
                    proto.getStatus(),
                    proto.getMessage(),
                    proto.hasData() ? UserRoleResponse.from(proto.getData()) : null
            );
        }
    }
}
