package com.sanedge.common.adapter.user;

import com.sanedge.common.adapter.model.User;

import io.smallrye.mutiny.Uni;

/**
 * Minimal cross-service user surface other domains need (e.g. merchant and card
 * resolving the owner of a record), replacing direct {@code @GrpcClient("user")}
 * usage.
 */
public interface UserPort {

    Uni<User> findById(int userId);
}
