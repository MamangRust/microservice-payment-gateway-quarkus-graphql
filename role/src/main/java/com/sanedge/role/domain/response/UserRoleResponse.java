package com.sanedge.role.domain.response;

import com.sanedge.role.entity.UserRole;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@RegisterForReflection
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleResponse {
    private Long userId;
    private Long roleId;
    private String roleName;

    public static UserRoleResponse from(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return UserRoleResponse.builder()
                .userId(userRole.getUserId())
                .roleId(userRole.getRole() != null ? userRole.getRole().id : null)
                .roleName(userRole.getRole() != null ? userRole.getRole().getRoleName() : null)
                .build();
    }
}
