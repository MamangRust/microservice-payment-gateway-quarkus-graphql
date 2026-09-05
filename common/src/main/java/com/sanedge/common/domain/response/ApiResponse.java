package com.sanedge.common.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponse<T>(
        String status,
        String message,
        T data) {

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>("success", message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>("error", message, null);
    }
}
