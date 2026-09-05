package com.sanedge.withdraw.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.sanedge.withdraw.entity.Outbox;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<Outbox> {

    @ConfigProperty(name = "outbox.lease.seconds", defaultValue = "300")
    long leaseSeconds;

    @WithTransaction
    public Uni<Long> countPending(String domain) {
        return count("status = 'PENDING' AND domain = ?1", domain);
    }

    @WithTransaction
    public Uni<List<Outbox>> findDue(String domain, int limit) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        return find("status = 'PENDING' AND domain = ?1 "
                + "AND (nextAttemptAt IS NULL OR nextAttemptAt <= ?2) "
                + "AND (claimedAt IS NULL OR claimedAt < ?3) ORDER BY createdAt", domain, now, leaseCutoff)
                .page(0, limit)
                .list();
    }

    @WithTransaction
    public Uni<Outbox> claim(String domain, Outbox event) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp leaseCutoff = Timestamp.valueOf(LocalDateTime.now().minusSeconds(Math.max(1, leaseSeconds)));
        String claimToken = UUID.randomUUID().toString();
        return update("claimedAt = ?1, claimToken = ?2, attempts = attempts + 1 "
                + "WHERE id = ?3 AND domain = ?4 AND status = 'PENDING' "
                + "AND (claimedAt IS NULL OR claimedAt < ?5) "
                + "AND (nextAttemptAt IS NULL OR nextAttemptAt <= ?1)", now, claimToken, event.getId(), domain,
                leaseCutoff)
                .chain(updated -> updated > 0 ? findById(event.getId()) : Uni.createFrom().nullItem());
    }

    @WithTransaction
    public Uni<Void> markSent(Outbox event) {
        return update("status = 'SENT', claimedAt = NULL, lastError = NULL "
                + "WHERE id = ?1 AND status = 'PENDING' AND claimToken = ?2", event.getId(), event.getClaimToken())
                .replaceWithVoid();
    }

    @WithTransaction
    public Uni<Void> markRetry(Outbox event, Throwable failure, long delaySeconds) {
        return update("status = 'PENDING', claimedAt = NULL, lastError = ?1, nextAttemptAt = ?2 "
                + "WHERE id = ?3 AND status = 'PENDING' AND claimToken = ?4",
                failure == null ? "unknown failure" : failure.getMessage(),
                Timestamp.valueOf(LocalDateTime.now().plusSeconds(delaySeconds)), event.getId(), event.getClaimToken())
                .replaceWithVoid();
    }
}
