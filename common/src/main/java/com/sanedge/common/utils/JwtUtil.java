package com.sanedge.common.utils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JwtUtil {

    @Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer")
    String jwtIssuer;

    @Inject
    @ConfigProperty(name = "app.jwtExpirationMs")
    int jwtExpirationMs;

    @Inject
    @ConfigProperty(name = "app.jwtRefreshExpirationMs")
    int jwtRefreshExpirationMs;

    @Inject
    JWTParser jwtParser;

    public String generateToken(String username, List<String> roles, Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);

        Set<String> groups = new HashSet<>(roles);
        groups.add("user");

        return Jwt.claims()
                .issuer(jwtIssuer)
                .subject(username)
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("userId", userId)
                .groups(groups)
                .sign();
    }

    public String generateRefreshToken(String username, Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtRefreshExpirationMs);

        return Jwt.claims()
                .issuer(jwtIssuer)
                .subject(username)
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("userId", userId)
                .claim("type", "refresh")
                .sign();
    }

    public boolean validateToken(String token) {
        try {
            jwtParser.parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            return jwt.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public List<String> getRolesFromToken(String token) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            Set<String> groups = jwt.getGroups();
            return groups != null ? groups.stream().collect(Collectors.toList()) : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public Long getUserIdFromToken(String token) {
        try {
            JsonWebToken jwt = jwtParser.parse(token);
            return jwt.getClaim("userId");
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpirationMs() {
        return jwtExpirationMs;
    }

    public long getRefreshExpirationMs() {
        return jwtRefreshExpirationMs;
    }
}