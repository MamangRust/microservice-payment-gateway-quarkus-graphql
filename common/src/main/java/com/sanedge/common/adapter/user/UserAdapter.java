package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.User;
import com.sanedge.common.adapter.support.ProtoTime;
import com.sanedge.common.exception.ResourceNotFoundException;

import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import pb.user.User.FindByIdUserRequest;
import pb.user.User.UserResponse;
import pb.user.UserQueryService;

@ApplicationScoped
public class UserAdapter implements UserPort {

    @GrpcClient("user")
    UserQueryService query;

    @Override
    public Uni<User> findById(int userId) {
        return query.findById(FindByIdUserRequest.newBuilder().setId(userId).build())
                .map(resp -> {
                    if (resp == null || !resp.hasData()) {
                        throw new ResourceNotFoundException("User not found: " + userId);
                    }
                    return toUser(resp.getData());
                });
    }

    static User toUser(UserResponse u) {
        if (u == null) {
            return null;
        }
        return new User(u.getId(), u.getFirstname(), u.getLastname(), u.getEmail(),
                ProtoTime.parse(u.getCreatedAt()), ProtoTime.parse(u.getUpdatedAt()));
    }
}
