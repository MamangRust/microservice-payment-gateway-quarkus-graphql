package com.sanedge.auth.repository;

import com.sanedge.auth.entity.RefreshToken;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RefreshTokenRepository implements PanacheRepository<RefreshToken> {
    
    public Uni<RefreshToken> findByToken(String token) {
        return find("token", token).firstResult();
    }

    public Uni<RefreshToken> findByUserId(Long userId) {
        return find("userId", userId).firstResult();
    }

    public Uni<Long> deleteByUserId(Long userId) {
        return delete("userId", userId);
    }

    public Uni<Long> deleteByToken(String token) {
        return delete("token", token);
    }
}
