package com.sanedge.role.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.role.domain.requests.CreateRoleRequest;
import com.sanedge.role.domain.requests.UpdateRoleRequest;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.role.domain.response.RoleResponse;
import com.sanedge.role.domain.response.RoleResponseDeleteAt;
import com.sanedge.role.domain.response.UserRoleResponse;
import com.sanedge.role.entity.Role;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.role.repository.RoleRepository;
import com.sanedge.role.repository.UserRoleRepository;
import com.sanedge.role.service.RoleCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RoleCommandServiceImpl implements RoleCommandService {
    private static final Logger logger = LoggerFactory.getLogger(RoleCommandServiceImpl.class);

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    @Inject
    public RoleCommandServiceImpl(RoleRepository roleRepository,
                                  UserRoleRepository userRoleRepository,
                                  RedisService redisService,
                                  TracingMetrics tracingMetrics) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<RoleResponse>> create(CreateRoleRequest request) {
        Attributes attrs = Attributes.builder().put("role.name", request.getName()).build();
        logger.info("Creating new role with name: {}", request.getName());

        return tracingMetrics.traceAndMeasure("createRole", "create_role", attrs, () -> {
            return roleRepository.findByRoleName(request.getName())
                    .chain(existingRole -> {
                        if (existingRole != null) {
                            logger.warn("Role creation failed - role already exists: {}", request.getName());
                            throw new ResourceAlreadyExistsException("Role with name '" + request.getName() + "' already exists");
                        }

                        Role newRole = new Role();
                        newRole.setRoleName(request.getName());
                        return roleRepository.persist(newRole)
                                .map(v -> {
                                    RoleResponse roleResponse = RoleResponse.from(newRole);
                                    logger.info("Role created. List caches will be refreshed upon expiry.");
                                    logger.info("Successfully created role with id: {} and name: {}", newRole.id, newRole.getRoleName());
                                    return ApiResponse.success("Role created successfully", roleResponse);
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<RoleResponse>> update(UpdateRoleRequest request) {
        Attributes attrs = Attributes.builder()
                .put("role.id", request.getRoleId())
                .put("role.new_name", request.getName())
                .build();
        logger.info("Updating role with id: {} to new name: {}", request.getRoleId(), request.getName());

        return tracingMetrics.traceAndMeasure("updateRole", "update_role", attrs, () -> {
            return roleRepository.findById(request.getRoleId().longValue())
                    .chain(existingRole -> {
                        if (existingRole == null) {
                            logger.warn("Role update failed - role not found with id: {}", request.getRoleId());
                            throw new ResourceNotFoundException("Role not found with id: " + request.getRoleId());
                        }

                        Uni<Role> updateFlow;
                        if (!existingRole.getRoleName().equals(request.getName())) {
                            updateFlow = roleRepository.findByRoleName(request.getName())
                                    .chain(duplicateRole -> {
                                        if (duplicateRole != null) {
                                            logger.warn("Role update failed - new name '{}' already exists for another role", request.getName());
                                            throw new ResourceAlreadyExistsException("Role with name '" + request.getName() + "' already exists");
                                        }
                                        existingRole.setRoleName(request.getName());
                                        return roleRepository.persist(existingRole).map(v -> existingRole);
                                    });
                        } else {
                            updateFlow = Uni.createFrom().item(existingRole);
                        }

                        return updateFlow.chain(updatedRole -> {
                            RoleResponse roleResponse = RoleResponse.from(updatedRole);
                            String cacheKey = "role:" + request.getRoleId();

                            return redisService.deleteReactive(cacheKey)
                                    .map(v -> {
                                        logger.info("Invalidated cache for key: {}", cacheKey);
                                        logger.info("Successfully updated role with id: {}", request.getRoleId());
                                        return ApiResponse.success("Role updated successfully", roleResponse);
                                    });
                        });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<RoleResponseDeleteAt>> trash(Long id) {
        Attributes attrs = Attributes.builder().put("role.id", id).build();
        logger.info("Trashing role with id: {}", id);

        return tracingMetrics.traceAndMeasure("trashRole", "trash_role", attrs, () -> {
            return roleRepository.trash(id)
                    .chain(trashedRole -> {
                        if (trashedRole == null) {
                            logger.warn("Role trash failed - role not found with id: {}", id);
                            throw new ResourceNotFoundException("Role not found with id: " + id);
                        }

                        RoleResponseDeleteAt roleResponseDeleteAt = RoleResponseDeleteAt.from(trashedRole);
                        String cacheKey = "role:" + id;

                        return redisService.deleteReactive(cacheKey)
                                .map(v -> {
                                    logger.info("Invalidated cache for key: {}", cacheKey);
                                    logger.info("Successfully trashed role with id: {}", id);
                                    return ApiResponse.success("Role trashed successfully", roleResponseDeleteAt);
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<RoleResponseDeleteAt>> restore(Long id) {
        Attributes attrs = Attributes.builder().put("role.id", id).build();
        logger.info("Restoring role with id: {}", id);

        return tracingMetrics.traceAndMeasure("restoreRole", "restore_role", attrs, () -> {
            return roleRepository.restore(id)
                    .chain(restoredRole -> {
                        if (restoredRole == null) {
                            logger.warn("Role restore failed - role not found or must be trashed first with id: {}", id);
                            throw new InvalidRequestException("Role not found or must be trashed first");
                        }

                        RoleResponseDeleteAt roleResponseDeleteAt = RoleResponseDeleteAt.from(restoredRole);
                        String cacheKey = "role:" + id;

                        return redisService.deleteReactive(cacheKey)
                                .map(v -> {
                                    logger.info("Invalidated cache for key: {}", cacheKey);
                                    logger.info("Successfully restored role with id: {}", id);
                                    return ApiResponse.success("Role restored successfully", roleResponseDeleteAt);
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deletePermanent(Long id) {
        Attributes attrs = Attributes.builder().put("role.id", id).build();
        logger.info("Permanently deleting role with id: {}", id);

        return tracingMetrics.traceAndMeasure("deleteRolePermanent", "delete_role_permanent", attrs, () -> {
            return roleRepository.deletePermanent(id)
                    .chain(deletedRole -> {
                        if (deletedRole == null) {
                            logger.warn("Permanent delete failed - role not found or must be trashed before permanent deletion with id: {}", id);
                            throw new InvalidRequestException("Role not found or must be trashed before permanent deletion");
                        }

                        String cacheKey = "role:" + id;
                        return redisService.deleteReactive(cacheKey)
                                .map(v2 -> {
                                    logger.info("Invalidated cache for key: {}", cacheKey);
                                    logger.info("Successfully permanently deleted role with id: {}", id);
                                    return ApiResponse.success("Role deleted permanently");
                                });
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> restoreAllTrashedRoles() {
        logger.info("Restoring all trashed roles");

        return tracingMetrics.traceAndMeasure("restoreAllTrashedRoles", "restore_all_trashed_roles", () -> {
            return roleRepository.restoreAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed roles found");
                        }
                        logger.warn("All trashed roles restored. Caches will be refreshed upon expiry or next access.");
                        logger.info("Successfully restored all trashed roles");
                        return ApiResponse.success("All trashed roles have been restored successfully");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> deleteAllTrashedRoles() {
        logger.info("Permanently deleting all trashed roles");

        return tracingMetrics.traceAndMeasure("deleteAllTrashedRoles", "delete_all_trashed_roles", () -> {
            return roleRepository.deleteAllDeleted()
                    .map(success -> {
                        if (!success) {
                            throw new ResourceNotFoundException("No trashed roles found");
                        }
                        logger.warn("All trashed roles deleted. Caches will be refreshed upon expiry or next access.");
                        logger.info("Successfully deleted all trashed roles");
                        return ApiResponse.success("All trashed roles have been deleted permanently");
                    });
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<UserRoleResponse>> assignRoleToUser(Long userId, Long roleId) {
        Attributes attrs = Attributes.builder()
                .put("user.id", userId)
                .put("role.id", roleId)
                .build();
        logger.info("Assigning role with id: {} to user with id: {}", roleId, userId);

        return tracingMetrics.traceAndMeasure("assignRoleToUser", "assign_role_to_user", attrs, () -> {
            return userRoleRepository.assignRole(userId, roleId, roleRepository)
                    .chain(userRole -> redisService.deleteReactive("roles:user:" + userId)
                            .map(v -> {
                                logger.info("Invalidated role cache for user id: {}", userId);
                                logger.info("Successfully assigned role with id: {} to user with id: {}", roleId, userId);
                                return ApiResponse.success("Role assigned to user successfully", UserRoleResponse.from(userRole));
                            }));
        });
    }

    @Override
    @WithTransaction
    public Uni<ApiResponse<Void>> removeRoleFromUser(Long userId, Long roleId) {
        Attributes attrs = Attributes.builder()
                .put("user.id", userId)
                .put("role.id", roleId)
                .build();
        logger.info("Removing role with id: {} from user with id: {}", roleId, userId);

        return tracingMetrics.traceAndMeasure("removeRoleFromUser", "remove_role_from_user", attrs, () -> {
            return userRoleRepository.removeRole(userId, roleId)
                    .chain(removed -> {
                        if (!removed) {
                            logger.warn("Role removal failed - association not found for user id: {} and role id: {}", userId, roleId);
                            throw new ResourceNotFoundException("UserRole association not found");
                        }
                        return redisService.deleteReactive("roles:user:" + userId)
                                .map(v -> {
                                    logger.info("Invalidated role cache for user id: {}", userId);
                                    logger.info("Successfully removed role with id: {} from user with id: {}", roleId, userId);
                                    return ApiResponse.<Void>success("Role removed from user successfully");
                                });
                    });
        });
    }
}