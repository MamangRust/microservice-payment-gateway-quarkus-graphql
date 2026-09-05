package com.sanedge.common.domain.response;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record PaginationMeta(
                int currentPage,
                int pageSize,
                int totalPages,
                int totalRecords) {
}
