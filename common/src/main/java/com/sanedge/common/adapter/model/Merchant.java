package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of a merchant owned by the merchant service.
 */
public record Merchant(
        int id,
        String name,
        String apiKey,
        String status,
        int userId,
        Instant createdAt,
        Instant updatedAt) {
}
