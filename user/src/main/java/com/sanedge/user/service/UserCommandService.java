package com.sanedge.user.service;

import com.sanedge.common.domain.response.ApiResponse;
import com.sanedge.user.domain.requests.RegisterRequest;
import com.sanedge.user.domain.requests.UpdateUserRequest;
import com.sanedge.user.domain.response.UserResponse;
import com.sanedge.user.domain.response.UserResponseDeleteAt;

import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;

public interface UserCommandService {
    Uni<ApiResponse<UserResponse>> createUser(RegisterRequest request);
    Uni<ApiResponse<UserResponse>> updateUser(@Valid UpdateUserRequest request);
    Uni<ApiResponse<UserResponseDeleteAt>> trashed(Long id);
    Uni<ApiResponse<UserResponseDeleteAt>> restore(Long id);
    Uni<ApiResponse<Void>> deletePermanent(Long id);
    Uni<ApiResponse<Void>> restoreAllTrashedUsers();
    Uni<ApiResponse<Void>> deleteAllTrashedUsers();
    Uni<ApiResponse<UserResponse>> verifyPassword(String email, String password);
}
