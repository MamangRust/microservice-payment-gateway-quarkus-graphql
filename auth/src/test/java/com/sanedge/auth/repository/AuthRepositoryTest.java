package com.sanedge.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import com.sanedge.auth.entity.RefreshToken;
import com.sanedge.auth.entity.ResetToken;
import com.sanedge.common.test.PostgreSqlResource;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@QuarkusTestResource(PostgreSqlResource.class)
@RunOnVertxContext
class AuthRepositoryTest {

    @Inject
    RefreshTokenRepository refreshTokenRepo;

    @Inject
    ResetTokenRepository resetTokenRepo;

    private Uni<RefreshToken> createRefreshToken(String token, Long userId) {
        RefreshToken rt = new RefreshToken();
        rt.setToken(token);
        rt.setUserId(userId);
        rt.setExpiration(new Timestamp(System.currentTimeMillis() + 3600000));
        return RefreshToken.persist(rt).replaceWith(rt);
    }

    private Uni<ResetToken> createResetToken(String token, Long userId) {
        ResetToken rt = new ResetToken();
        rt.setToken(token);
        rt.setUserId(userId);
        rt.setExpiration(new Timestamp(System.currentTimeMillis() + 900000));
        return ResetToken.persist(rt).replaceWith(rt);
    }

    private Uni<Void> cleanUpRefreshTokens() {
        return RefreshToken.deleteAll().replaceWithVoid();
    }

    private Uni<Void> cleanUpResetTokens() {
        return ResetToken.deleteAll().replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> persistRefreshToken() {
        return cleanUpRefreshTokens()
                .chain(() -> {
                    RefreshToken token = new RefreshToken();
                    token.setToken("test-refresh-token");
                    token.setUserId(1L);
                    token.setExpiration(new Timestamp(System.currentTimeMillis() + 3600000));
                    return RefreshToken.persist(token).replaceWith(token);
                })
                .invoke(saved -> assertThat(saved.id).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> findRefreshTokenByToken() {
        return cleanUpRefreshTokens()
                .chain(() -> createRefreshToken("find-me-token", 1L))
                .chain(ignored -> refreshTokenRepo.findByToken("find-me-token"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getToken()).isEqualTo("find-me-token");
                    assertThat(found.getUserId()).isEqualTo(1L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> findRefreshTokenByUserId() {
        return cleanUpRefreshTokens()
                .chain(() -> createRefreshToken("user-token", 2L))
                .chain(ignored -> refreshTokenRepo.findByUserId(2L))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getUserId()).isEqualTo(2L);
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> deleteRefreshTokenByUserId() {
        return cleanUpRefreshTokens()
                .chain(() -> createRefreshToken("delete-by-user", 3L))
                .chain(ignored -> refreshTokenRepo.deleteByUserId(3L))
                .invoke(deleted -> assertThat(deleted).isPositive())
                .chain(ignored -> refreshTokenRepo.findByUserId(3L))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> deleteRefreshTokenByToken() {
        return cleanUpRefreshTokens()
                .chain(() -> createRefreshToken("delete-by-token", 4L))
                .chain(ignored -> refreshTokenRepo.deleteByToken("delete-by-token"))
                .invoke(deleted -> assertThat(deleted).isPositive())
                .chain(ignored -> refreshTokenRepo.findByToken("delete-by-token"))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> persistResetToken() {
        return cleanUpResetTokens()
                .chain(() -> {
                    ResetToken rt = new ResetToken();
                    rt.setToken("reset-token-001");
                    rt.setUserId(5L);
                    rt.setExpiration(new Timestamp(System.currentTimeMillis() + 900000));
                    return ResetToken.persist(rt).replaceWith(rt);
                })
                .invoke(saved -> assertThat(saved.id).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> findResetTokenByToken() {
        return cleanUpResetTokens()
                .chain(() -> createResetToken("find-reset-token", 6L))
                .chain(ignored -> resetTokenRepo.findByToken("find-reset-token"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getToken()).isEqualTo("find-reset-token");
                })
                .replaceWithVoid();
    }

    @Test
    @WithSession
    Uni<Void> deleteResetTokenByUserId() {
        return cleanUpResetTokens()
                .chain(() -> createResetToken("reset-delete-user", 7L))
                .chain(ignored -> resetTokenRepo.deleteByUserId(7L))
                .invoke(deleted -> assertThat(deleted).isPositive())
                .chain(ignored -> resetTokenRepo.findByToken("reset-delete-user"))
                .invoke(found -> assertThat(found).isNull())
                .replaceWithVoid();
    }
}
