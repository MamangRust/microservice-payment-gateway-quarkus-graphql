package com.sanedge.gateway.security;

import com.sanedge.common.utils.JwtUtil;
import com.sanedge.gateway.dto.UserDto;
import com.sanedge.gateway.service.UserService;

import io.quarkus.grpc.GrpcClient;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.core.http.HttpServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
public class SecurityContext {
    private static final Logger LOG = LoggerFactory.getLogger(SecurityContext.class);

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    UserService userService;

    @Inject
    JwtUtil jwtUtil;

    @GrpcClient("role")
    pb.role.MutinyRoleServiceGrpc.MutinyRoleServiceStub roleQueryService;

    @Inject
    HttpServerRequest request;

    public Uni<Long> getCurrentUserId() {
        if (securityIdentity.isAnonymous()) {
            LOG.debug("Access attempt by anonymous user.");
            return Uni.createFrom().nullItem();
        }

        try {
            // Attempt to retrieve from security identity attributes mapped in JwtIdentityProvider
            Object userIdAttr = securityIdentity.getAttribute("userId");
            if (userIdAttr != null) {
                if (userIdAttr instanceof Number number) {
                    LOG.debug("Found user ID in security attributes: {}", number.longValue());
                    return Uni.createFrom().item(number.longValue());
                }
                try {
                    long userId = Long.parseLong(userIdAttr.toString());
                    LOG.debug("Found user ID in security attributes (parsed): {}", userId);
                    return Uni.createFrom().item(userId);
                } catch (NumberFormatException e) {
                    LOG.warn("Failed to parse userId attribute: {}", userIdAttr);
                }
            }

            // Fallback to username search if attribute is not populated
            String username = securityIdentity.getPrincipal().getName();
            LOG.debug("Username from security identity principal: {}", username);

            return getUserIdFromHeader();
        } catch (Exception e) {
            LOG.error("An error occurred while trying to get the current user ID", e);
            return Uni.createFrom().nullItem();
        }
    }

    private Uni<Long> getUserIdFromHeader() {
        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            LOG.debug("Attempting to parse user ID from Authorization header.");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtUtil.getUserIdFromToken(token);
                LOG.debug("User ID parsed from token: {}", userId);

                if (userId != null) {
                    return Uni.createFrom().item(userId);
                }
            }
        }
        return Uni.createFrom().nullItem();
    }

    public Uni<UserDto.UserResponse> getCurrentUser() {
        return getCurrentUserId()
                .chain(userId -> {
                    if (userId != null) {
                        return userService.getUser(userId.intValue())
                                .map(apiResponse -> {
                                    if (apiResponse != null && apiResponse.data() != null) {
                                        return apiResponse.data();
                                    }
                                    return null;
                                });
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    public Uni<Boolean> hasRole(String role) {
        return getCurrentUserId()
                .chain(userId -> {
                    if (userId == null) {
                        return Uni.createFrom().item(false);
                    }
                    return roleQueryService.findByUserId(pb.role.Role.FindByIdUserRoleRequest.newBuilder()
                            .setUserId(userId.intValue())
                            .build())
                            .map(response -> {
                                if (response == null || response.getDataList() == null) {
                                    return false;
                                }
                                return response.getDataList().stream()
                                        .anyMatch(r -> r.getName().equalsIgnoreCase(role));
                            })
                            .onFailure().recoverWithItem(false);
                });
    }

    public Uni<Boolean> hasPermission(String permission) {
        return getCurrentUserId()
                .chain(userId -> {
                    if (userId == null) {
                        return Uni.createFrom().item(false);
                    }
                    return roleQueryService.findByUserId(pb.role.Role.FindByIdUserRoleRequest.newBuilder()
                            .setUserId(userId.intValue())
                            .build())
                            .map(response -> {
                                if (response == null || response.getDataList() == null) {
                                    return false;
                                }
                                return response.getDataList().stream()
                                        .anyMatch(role -> hasRolePermission(role.getName(), permission));
                            })
                            .onFailure().recoverWithItem(false);
                });
    }

    private boolean hasRolePermission(String roleName, String requiredPermission) {
        return switch (roleName) {
            case "ROLE_ADMIN" -> true;
            case "ROLE_USER_MANAGER" ->
                List.of("USER_READ", "USER_WRITE", "USER_DELETE").contains(requiredPermission);
            case "ROLE_MANAGER" -> List.of("ROLE_READ", "ROLE_WRITE").contains(requiredPermission);
            case "ROLE_USER" -> List.of("PROFILE_READ", "PROFILE_WRITE").contains(requiredPermission);
            default -> false;
        };
    }
}
