package com.sanedge.common.adapter.user;

import java.util.Optional;

import com.sanedge.common.adapter.model.User;

import io.smallrye.mutiny.Uni;

/**
 * Auth-facing user surface: credential lookups plus the account mutations the
 * auth flows perform. Replaces direct {@code @GrpcClient("user")} usage in the
 * auth service.
 */
public interface AuthUserPort {

    Uni<User> findById(int userId);

    Uni<Optional<User>> findByEmail(String email);

    Uni<User> create(RegisterData data);

    Uni<VerifyResult> verifyPassword(String email, String password);

    Uni<User> update(UpdateData data);

    record RegisterData(
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {
    }

    record UpdateData(
            int id,
            String firstname,
            String lastname,
            String email,
            String password,
            String confirmPassword) {
    }

    record VerifyResult(boolean valid, User user) {
    }
}
