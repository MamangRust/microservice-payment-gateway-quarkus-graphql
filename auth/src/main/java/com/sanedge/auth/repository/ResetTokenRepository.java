package com.sanedge.auth.repository;

import com.sanedge.auth.entity.ResetToken;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ResetTokenRepository implements PanacheRepository<ResetToken> {

    public Uni<ResetToken> findByToken(String token) {
        return find("token", token).firstResult();
    }

    public Uni<Long> deleteByUserId(Long userId) {
        return delete("userId", userId);
    }
}
