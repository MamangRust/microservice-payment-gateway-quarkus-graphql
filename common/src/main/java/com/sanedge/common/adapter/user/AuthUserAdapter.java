package com.sanedge.common.adapter.user;

import java.util.Optional;

import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.support.AdapterException;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;
import pb.user.UserCommand.VerifyPasswordRequest;
import pb.user.UserCommandService;
import pb.user.UserQueryService;

@ApplicationScoped
public class AuthUserAdapter implements AuthUserPort {

    @GrpcClient("user")
    UserQueryService query;

    @GrpcClient("user")
    UserCommandService command;

    @Override
    public Uni<User> findById(int userId) {
        return query.findById(FindByIdUserRequest.newBuilder().setId(userId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("User not found: " + userId);
                    }
                    return UserAdapter.toUser(resp.getData());
                });
    }

    @Override
    public Uni<Optional<User>> findByEmail(String email) {
        return query.findAll(FindAllUserRequest.newBuilder()
                .setSearch(email)
                .setPage(1)
                .setPageSize(1)
                .build())
                .map(resp -> {
                    if (resp == null) {
                        return Optional.empty();
                    }
                    for (UserResponse u : resp.getDataList()) {
                        if (u.getEmail().equalsIgnoreCase(email)) {
                            return Optional.of(UserAdapter.toUser(u));
                        }
                    }
                    return Optional.empty();
                });
    }

    @Override
    public Uni<User> create(RegisterData data) {
        return command.create(CreateUserRequest.newBuilder()
                .setFirstname(nullSafe(data.firstname()))
                .setLastname(nullSafe(data.lastname()))
                .setEmail(nullSafe(data.email()))
                .setPassword(nullSafe(data.password()))
                .setConfirmPassword(nullSafe(data.confirmPassword()))
                .build())
                .map(resp -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException(resp == null ? "create user failed"
                                : "create user failed: " + resp.getMessage());
                    }
                    if (!resp.hasData()) {
                        throw new AdapterException("create user returned no data");
                    }
                    return UserAdapter.toUser(resp.getData());
                });
    }

    @Override
    public Uni<VerifyResult> verifyPassword(String email, String password) {
        return command.verifyPassword(VerifyPasswordRequest.newBuilder()
                .setEmail(nullSafe(email))
                .setPassword(nullSafe(password))
                .build())
                .map(resp -> {
                    if (resp == null) {
                        return new VerifyResult(false, null);
                    }
                    User user = resp.hasUser() ? UserAdapter.toUser(resp.getUser()) : null;
                    return new VerifyResult(resp.getValid(), user);
                });
    }

    @Override
    public Uni<User> update(UpdateData data) {
        return command.update(UpdateUserRequest.newBuilder()
                .setId(data.id())
                .setFirstname(nullSafe(data.firstname()))
                .setLastname(nullSafe(data.lastname()))
                .setEmail(nullSafe(data.email()))
                .setPassword(nullSafe(data.password()))
                .setConfirmPassword(nullSafe(data.confirmPassword()))
                .build())
                .map(resp -> {
                    if (resp == null || !"success".equalsIgnoreCase(resp.getStatus())) {
                        throw new AdapterException(resp == null ? "update user failed"
                                : "update user failed: " + resp.getMessage());
                    }
                    if (!resp.hasData()) {
                        throw new AdapterException("update user returned no data");
                    }
                    return UserAdapter.toUser(resp.getData());
                });
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
