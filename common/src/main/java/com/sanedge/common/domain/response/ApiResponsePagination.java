package com.sanedge.common.domain.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ApiResponsePagination<T>(
        String status,
        String message,
        T data,
        PaginationMeta pagination) {
}
