package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of a user owned by the user service.
 */
public record User(
        int id,
        String firstname,
        String lastname,
        String email,
        Instant createdAt,
        Instant updatedAt) {
}
