package com.sanedge.common.adapter.support;

import java.util.List;

/**
 * Result of a paginated adapter read: the page items plus the total record
 * count reported by the remote service.
 */
public record Paged<T>(List<T> items, int total) {

    public static <T> Paged<T> of(List<T> items, int total) {
        return new Paged<>(items, total);
    }

    public static <T> Paged<T> empty() {
        return new Paged<>(List.of(), 0);
    }
}
