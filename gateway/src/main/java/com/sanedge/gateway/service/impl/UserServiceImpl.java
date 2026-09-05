package com.sanedge.gateway.service.impl;

import com.sanedge.gateway.dto.UserDto.*;
import com.sanedge.gateway.service.UserService;
import com.sanedge.gateway.telemetry.TelemetryHelper;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class UserServiceImpl implements UserService {

    private static final Logger LOG = Logger.getLogger(UserServiceImpl.class);

    @Inject
    TelemetryHelper telemetryHelper;

    @GrpcClient("user")
    pb.user.MutinyUserQueryServiceGrpc.MutinyUserQueryServiceStub userQueryService;

    @GrpcClient("user")
    pb.user.MutinyUserCommandServiceGrpc.MutinyUserCommandServiceStub userCommandService;

    @Override
    public Uni<FindAllUserResponse> listUsers(int page, int size, String search) {
        return telemetryHelper.traceAndMetric("user.listUsers", () -> userQueryService.findAll(pb.user.User.FindAllUserRequest.newBuilder()
                .setPage(page)
                .setPageSize(size)
                .setSearch(search == null ? "" : search)
                .build())
                .map(FindAllUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to list users: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<FindByIdUserResponse> getUser(int id) {
        return telemetryHelper.traceAndMetric("user.getUser", () -> userQueryService.findById(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(FindByIdUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to get user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<CreateUserResponse> createUser(CreateUserRequest body) {
        return telemetryHelper.traceAndMetric("user.createUser", () -> userCommandService.create(pb.user.UserCommand.CreateUserRequest.newBuilder()
                .setFirstname(body.firstname() == null ? "" : body.firstname())
                .setLastname(body.lastname() == null ? "" : body.lastname())
                .setEmail(body.email() == null ? "" : body.email())
                .setPassword(body.password() == null ? "" : body.password())
                .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                .build())
                .map(CreateUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to create user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<UpdateUserResponse> updateUser(int id, UpdateUserRequest body) {
        return telemetryHelper.traceAndMetric("user.updateUser", () -> userCommandService.update(pb.user.UserCommand.UpdateUserRequest.newBuilder()
                .setId(id)
                .setFirstname(body.firstname() == null ? "" : body.firstname())
                .setLastname(body.lastname() == null ? "" : body.lastname())
                .setEmail(body.email() == null ? "" : body.email())
                .setPassword(body.password() == null ? "" : body.password())
                .setConfirmPassword(body.confirmPassword() == null ? "" : body.confirmPassword())
                .build())
                .map(UpdateUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to update user: " + throwable.getMessage(), throwable)));
    }

    @Override
    public Uni<TrashedUserResponse> deleteUser(int id) {
        return telemetryHelper.traceAndMetric("user.deleteUser", () -> userCommandService.trashedUser(pb.user.User.FindByIdUserRequest.newBuilder()
                .setId(id)
                .build())
                .map(TrashedUserResponse::from)
                .onFailure().invoke(throwable -> LOG.error("Failed to soft-delete user: " + throwable.getMessage(), throwable)));
    }
}
