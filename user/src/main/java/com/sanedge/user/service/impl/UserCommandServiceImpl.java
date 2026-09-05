package com.sanedge.user.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;
import com.sanedge.common.observability.TracingMetrics;
import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.common.exception.InvalidRequestException;
import com.sanedge.common.exception.ResourceAlreadyExistsException;
import com.sanedge.common.exception.ResourceNotFoundException;
import com.sanedge.common.utils.PasswordUtil;
import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.domain.requests.UpdateUserRequest;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;
import com.sanedge.user.entity.Role;
import com.sanedge.user.entity.User;
import com.sanedge.user.repository.UserRepository;
import com.sanedge.user.service.UserCommandService;

import io.opentelemetry.api.common.Attributes;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;

@ApplicationScoped
public class UserCommandServiceImpl implements UserCommandService {
        private static final Logger logger = LoggerFactory.getLogger(UserCommandServiceImpl.class);

        private final UserRepository userRepository;
        private final PasswordUtil passwordUtil;
        private final RedisService redisService;
        private final TracingMetrics tracingMetrics;

        @GrpcClient("role")
        pb.role.RoleService roleQueryService;

        @Inject
        public UserCommandServiceImpl(UserRepository userRepository,
                        PasswordUtil passwordUtil,
                        RedisService redisService,
                        TracingMetrics tracingMetrics) {
                this.userRepository = userRepository;
                this.passwordUtil = passwordUtil;
                this.redisService = redisService;
                this.tracingMetrics = tracingMetrics;
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponse>> createUser(RegisterRequest request) {
                if (!request.getPassword().equals(request.getConfirmPassword())) {
                        logger.warn("User creation failed - passwords do not match for username: {}",
                                        request.getUsername());
                        throw new InvalidRequestException("Passwords do not match");
                }

                Attributes attrs = Attributes.builder()
                                .put("user.username", request.getUsername())
                                .put("user.email", request.getEmail())
                                .build();

                logger.info("Creating new user with username: {}", request.getUsername());

                return tracingMetrics.traceAndMeasure("createUser", "create_user", attrs, () -> {
                        return userRepository.existsByUsername(request.getUsername())
                                        .chain(usernameExists -> {
                                                if (usernameExists) {
                                                        logger.warn("User creation failed - username already exists: {}",
                                                                        request.getUsername());
                                                        throw new ResourceAlreadyExistsException(
                                                                        "Username already exists");
                                                }
                                                return userRepository.existsByEmail(request.getEmail());
                                        })
                                        .chain(emailExists -> {
                                                if (emailExists) {
                                                        logger.warn("User creation failed - email already exists: {}",
                                                                        request.getEmail());
                                                        throw new ResourceAlreadyExistsException(
                                                                        "Email already exists");
                                                }

                                                User user = new User();
                                                user.setUsername(request.getUsername());
                                                user.setEmail(request.getEmail());
                                                user.setFirstname(request.getFirstname());
                                                user.setLastname(request.getLastname());
                                                user.setPassword(passwordUtil.hashPassword(request.getPassword()));

                                                Uni<Set<Role>> rolesUni;
                                                if (request.getRoleNames() != null
                                                                && !request.getRoleNames().isEmpty()) {
                                                        List<Uni<Role>> roleUnis = request.getRoleNames().stream()
                                                                        .map(this::resolveRoleViaGrpc)
                                                                        .collect(Collectors.toList());
                                                        rolesUni = Uni.join().all(roleUnis).andFailFast()
                                                                        .map(roles -> roles.stream()
                                                                                        .filter(java.util.Objects::nonNull)
                                                                                        .collect(Collectors.toSet()));
                                                } else {
                                                        rolesUni = Uni.createFrom().item(Set.of());
                                                }

                                                return rolesUni.chain(rolesToAssign -> {
                                                        if (rolesToAssign.isEmpty()) {
                                                                return resolveRoleViaGrpc("ROLE_USER")
                                                                                .map(adminRole -> {
                                                                                        Set<Role> roles = new java.util.HashSet<>();
                                                                                        if (adminRole != null) {
                                                                                                roles.add(adminRole);
                                                                                        }
                                                                                        return roles;
                                                                                });
                                                        }
                                                        return Uni.createFrom().item(rolesToAssign);
                                                }).chain(rolesToAssign -> {
                                                        user.setRoles(rolesToAssign);
                                                        return userRepository.persist(user)
                                                                        .map(v -> {
                                                                                UserResponse userResponse = UserResponse
                                                                                                .from(user);
                                                                                logger.info("Successfully created user with id: {} and username: {}",
                                                                                                user.id,
                                                                                                user.getUsername());
                                                                                logger.info("User created. List caches will be refreshed upon expiry.");
                                                                                return ApiResponse.success(
                                                                                                "User registered successfully",
                                                                                                userResponse);
                                                                        });
                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponse>> updateUser(@Valid UpdateUserRequest request) {
                Attributes attrs = Attributes.builder().put("user.id", request.getId()).build();
                logger.info("Updating user with id: {}", request.getId());

                return tracingMetrics.traceAndMeasure("updateUser", "update_user", attrs, () -> {
                        return userRepository.findById(request.getId())
                                        .chain(existingUser -> {
                                                if (existingUser == null) {
                                                        logger.warn("User update failed - user not found with id: {}",
                                                                        request.getId());
                                                        throw new ResourceNotFoundException(
                                                                        "User not found with id: " + request.getId());
                                                }

                                                Uni<Void> checkUsernameFlow;
                                                if (request.getUsername() != null && !request.getUsername()
                                                                .equals(existingUser.getUsername())) {
                                                        checkUsernameFlow = userRepository
                                                                        .existsByUsername(request.getUsername())
                                                                        .map(exists -> {
                                                                                if (exists) {
                                                                                        logger.warn("User update failed - username '{}' already in use",
                                                                                                        request.getUsername());
                                                                                        throw new ResourceAlreadyExistsException(
                                                                                                        "Username '" + request
                                                                                                                        .getUsername()
                                                                                                                        + "' is already in use");
                                                                                }
                                                                                existingUser.setUsername(
                                                                                                request.getUsername());
                                                                                return null;
                                                                        });
                                                } else {
                                                        checkUsernameFlow = Uni.createFrom().nullItem();
                                                }

                                                return checkUsernameFlow.chain(v -> {
                                                        Uni<Void> checkEmailFlow;
                                                        if (request.getEmail() != null && !request.getEmail()
                                                                        .equals(existingUser.getEmail())) {
                                                                checkEmailFlow = userRepository
                                                                                .existsByEmail(request.getEmail())
                                                                                .map(exists -> {
                                                                                        if (exists) {
                                                                                                logger.warn("User update failed - email '{}' already in use",
                                                                                                                request.getEmail());
                                                                                                throw new ResourceAlreadyExistsException(
                                                                                                                "Email '" + request
                                                                                                                                .getEmail()
                                                                                                                                + "' is already in use");
                                                                                        }
                                                                                        existingUser.setEmail(request
                                                                                                        .getEmail());
                                                                                        return null;
                                                                                });
                                                        } else {
                                                                checkEmailFlow = Uni.createFrom().nullItem();
                                                        }
                                                        return checkEmailFlow;
                                                }).chain(v -> {
                                                        if (request.getPassword() != null) {
                                                                if (!request.getPassword()
                                                                                .equals(request.getConfirmPassword())) {
                                                                        logger.warn("User update failed - passwords do not match for user id: {}",
                                                                                        request.getId());
                                                                        throw new InvalidRequestException(
                                                                                        "Passwords do not match");
                                                                }
                                                                existingUser.setPassword(passwordUtil
                                                                                .hashPassword(request.getPassword()));
                                                        }

                                                        if (request.getFirstname() != null) {
                                                                existingUser.setFirstname(request.getFirstname());
                                                        }
                                                        if (request.getLastname() != null) {
                                                                existingUser.setLastname(request.getLastname());
                                                        }

                                                        Uni<Void> rolesFlow;
                                                        if (request.getRoleNames() != null) {
                                                                List<Uni<Role>> roleUnis = request.getRoleNames()
                                                                                .stream()
                                                                                .map(this::resolveRoleViaGrpc)
                                                                                .collect(Collectors.toList());
                                                                rolesFlow = Uni.join().all(roleUnis).andFailFast()
                                                                                .map(roles -> {
                                                                                        existingUser.getRoles().clear();
                                                                                        existingUser.getRoles()
                                                                                                        .addAll(roles);
                                                                                        return null;
                                                                                });
                                                        } else {
                                                                rolesFlow = Uni.createFrom().nullItem();
                                                        }

                                                        return rolesFlow.chain(v3 -> {
                                                                existingUser.setUpdatedAt(
                                                                                Timestamp.valueOf(LocalDateTime.now()));
                                                                return userRepository.persist(existingUser)
                                                                                .chain(v4 -> {
                                                                                        UserResponse userResponse = UserResponse
                                                                                                        .from(existingUser);
                                                                                        String cacheKey = "user:"
                                                                                                        + request.getId();

                                                                                        return redisService
                                                                                                        .deleteReactive(cacheKey)
                                                                                                        .map(v5 -> {
                                                                                                                logger.info("Invalidated cache for key: {}",
                                                                                                                                cacheKey);
                                                                                                                logger.info("Successfully updated user with id: {}",
                                                                                                                                request.getId());
                                                                                                                return ApiResponse
                                                                                                                                .success("User updated successfully",
                                                                                                                                                userResponse);
                                                                                                        });
                                                                                });
                                                        });
                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponseDeleteAt>> trashed(Long id) {
                Attributes attrs = Attributes.builder().put("user.id", id).build();
                logger.info("Trashing user with id: {}", id);

                return tracingMetrics.traceAndMeasure("trashUser", "trash_user", attrs, () -> {
                        return userRepository.trash(id)
                                        .chain(trashedUser -> {
                                                if (trashedUser == null) {
                                                        logger.warn("User trash failed - user not found with id: {}",
                                                                        id);
                                                        throw new ResourceNotFoundException(
                                                                        "Trashed user not found with id: " + id);
                                                }

                                                UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt
                                                                .from(trashedUser);
                                                String cacheKey = "user:" + id;

                                                return redisService.deleteReactive(cacheKey)
                                                                .map(v -> {
                                                                        logger.info("Invalidated cache for key: {}",
                                                                                        cacheKey);
                                                                        logger.info("Successfully trashed user with id: {}",
                                                                                        id);
                                                                        return ApiResponse.success(
                                                                                        "User trashed successfully",
                                                                                        userResponseDeleteAt);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponseDeleteAt>> restore(Long id) {
                Attributes attrs = Attributes.builder().put("user.id", id).build();
                logger.info("Restoring user with id: {}", id);

                return tracingMetrics.traceAndMeasure("restoreUser", "restore_user", attrs, () -> {
                        return userRepository.restore(id)
                                        .chain(restoredUser -> {
                                                if (restoredUser == null) {
                                                        logger.warn("User restore failed - user not found or must be trashed first with id: {}",
                                                                        id);
                                                        throw new InvalidRequestException(
                                                                        "User not found or must be trashed first");
                                                }

                                                UserResponseDeleteAt userResponseDeleteAt = UserResponseDeleteAt
                                                                .from(restoredUser);
                                                String cacheKey = "user:" + id;

                                                return redisService.deleteReactive(cacheKey)
                                                                .map(v -> {
                                                                        logger.info("Invalidated cache for key: {}",
                                                                                        cacheKey);
                                                                        logger.info("Successfully restored user with id: {}",
                                                                                        id);
                                                                        return ApiResponse.success(
                                                                                        "User restored successfully",
                                                                                        userResponseDeleteAt);
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deletePermanent(Long id) {
                Attributes attrs = Attributes.builder().put("user.id", id).build();
                logger.info("Permanently deleting user with id: {}", id);

                return tracingMetrics.traceAndMeasure("deleteUserPermanent", "delete_user_permanent", attrs, () -> {
                        return userRepository.deletePermanent(id)
                                        .chain(deletedUser -> {
                                                if (deletedUser == null) {
                                                        logger.warn("Permanent delete failed - user not found or must be trashed before permanent deletion with id: {}",
                                                                        id);
                                                        throw new InvalidRequestException(
                                                                        "User not found or must be trashed before permanent deletion");
                                                }

                                                String cacheKey = "user:" + id;
                                                return redisService.deleteReactive(cacheKey)
                                                                .map(v2 -> {
                                                                        logger.info("Invalidated cache for key: {}",
                                                                                        cacheKey);
                                                                        logger.info("Successfully permanently deleted user with id: {}",
                                                                                        id);
                                                                        return ApiResponse.success(
                                                                                        "User deleted permanently");
                                                                });
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> restoreAllTrashedUsers() {
                logger.info("Restoring all trashed users");

                return tracingMetrics.traceAndMeasure("restoreAllTrashedUsers", "restore_all_trashed_users", () -> {
                        return userRepository.restoreAllDeleted()
                                        .map(success -> {
                                                if (!success) {
                                                        throw new ResourceNotFoundException("No trashed users found");
                                                }
                                                logger.warn("All trashed users restored. Caches will be refreshed upon expiry or next access.");
                                                logger.info("Successfully restored all trashed users");
                                                return ApiResponse.success(
                                                                "All trashed users have been restored successfully");
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<Void>> deleteAllTrashedUsers() {
                logger.info("Permanently deleting all trashed users");

                return tracingMetrics.traceAndMeasure("deleteAllTrashedUsers", "delete_all_trashed_users", () -> {
                        return userRepository.deleteAllDeleted()
                                        .map(success -> {
                                                if (!success) {
                                                        throw new ResourceNotFoundException("No trashed users found");
                                                }
                                                logger.warn("All trashed users deleted. Caches will be refreshed upon expiry or next access.");
                                                logger.info("Successfully deleted all trashed users");
                                                return ApiResponse.success(
                                                                "All trashed users have been deleted permanently");
                                        });
                });
        }

        @Override
        @WithTransaction
        public Uni<ApiResponse<UserResponse>> verifyPassword(String email, String password) {
                Attributes attrs = Attributes.builder().put("user.email", email).build();
                logger.info("Verifying password for user email: {}", email);

                return tracingMetrics.traceAndMeasure("verifyPassword", "verify_password", attrs, () -> {
                        return userRepository.findByEmail(email)
                                        .chain(user -> {
                                                if (user == null) {
                                                        logger.warn("Password verification failed - user not found with email: {}",
                                                                        email);
                                                        throw new ResourceNotFoundException(
                                                                        "User not found with email: " + email);
                                                }

                                                if (!passwordUtil.verifyPassword(password, user.getPassword())) {
                                                        logger.warn("Password verification failed - password mismatch for email: {}",
                                                                        email);
                                                        throw new InvalidRequestException("Invalid password");
                                                }

                                                UserResponse userResponse = UserResponse.from(user);
                                                logger.info("Successfully verified password for user email: {}", email);
                                                return Uni.createFrom().item(ApiResponse.success(
                                                                "Password verified successfully", userResponse));
                                        });
                });
        }

        private Uni<Role> resolveRoleViaGrpc(String roleName) {
                return roleQueryService.findByNameRole(pb.role.RoleQuery.FindByNameRoleRequest.newBuilder()
                                .setName(roleName)
                                .build())
                                .chain(response -> {
                                        if (!response.hasData()) {
                                                return Uni.createFrom().failure(new ResourceNotFoundException(
                                                                "Role '" + roleName + "' not found in Role service"));
                                        }
                                        pb.role.Role.RoleResponse matchedRole = response.getData();
                                        Role role = new Role();
                                        role.id = (long) matchedRole.getId();
                                        role.setRoleName(matchedRole.getName());
                                        return Uni.createFrom().item(role);
                                });
        }
}