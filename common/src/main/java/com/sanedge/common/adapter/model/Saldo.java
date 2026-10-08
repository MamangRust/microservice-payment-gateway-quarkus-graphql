package com.sanedge.common.adapter.model;

import java.time.Instant;

/**
 * Domain view of a balance row owned by the saldo service.
 */
public record Saldo(
        int saldoId,
        String cardNumber,
        int totalBalance,
        Integer withdrawAmount,
        Instant withdrawTime,
        Instant createdAt,
        Instant updatedAt) {
}
