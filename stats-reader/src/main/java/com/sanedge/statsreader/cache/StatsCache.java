package com.sanedge.statsreader.cache;

import java.util.function.Supplier;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.config.RedisService;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Cache-aside helper for the stats-reader (ClickHouse-backed) queries.
 *
 * <p>Every key is namespaced under {@code apigw:stats:} — the gateway-facing
 * stats cache must be separated from the domain service caches (keys like
 * {@code merchant:id:}, {@code transactions:all:} …). Without the namespace the
 * gateway/stats layer would clobber domain cache entries with its own shapes
 * (empty fields → 500), the lesson captured in the Go reference §8.1.
 *
 * <p>Cache reads are fail-open: if Redis is down the ClickHouse query still
 * runs, so a cache outage never takes the stats API down.
 */
@ApplicationScoped
public class StatsCache {

    private static final Logger log = LoggerFactory.getLogger(StatsCache.class);

    private static final String PREFIX = "apigw:stats:";

    @Inject
    RedisService redisService;

    @ConfigProperty(name = "stats.cache.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "stats.cache.ttl-seconds", defaultValue = "300")
    long ttlSeconds;

    /** Caches a JsonArray result (monthly/yearly aggregates, merchant lists). */
    public Uni<JsonArray> cacheArray(String key, Supplier<Uni<JsonArray>> loader) {
        if (!enabled) {
            return loader.get();
        }
        String cacheKey = PREFIX + key;
        return redisService.getReactive(cacheKey)
                .flatMap(cached -> {
                    if (cached != null) {
                        return Uni.createFrom().item(new JsonArray(cached));
                    }
                    return loader.get()
                            .flatMap(result -> redisService
                                    .setWithExpirationReactive(cacheKey, result.encode(), ttlSeconds)
                                    .replaceWith(result));
                })
                .onFailure().recoverWithUni(err -> {
                    log.warn("Stats cache read failed for {}: {} — falling back to ClickHouse", key, err.getMessage());
                    return loader.get();
                });
    }

    /** Caches a JsonObject result (card dashboard). */
    public Uni<JsonObject> cacheObject(String key, Supplier<Uni<JsonObject>> loader) {
        if (!enabled) {
            return loader.get();
        }
        String cacheKey = PREFIX + key;
        return redisService.getReactive(cacheKey)
                .flatMap(cached -> {
                    if (cached != null) {
                        return Uni.createFrom().item(new JsonObject(cached));
                    }
                    return loader.get()
                            .flatMap(result -> redisService
                                    .setWithExpirationReactive(cacheKey, result.encode(), ttlSeconds)
                                    .replaceWith(result));
                })
                .onFailure().recoverWithUni(err -> {
                    log.warn("Stats cache read failed for {}: {} — falling back to ClickHouse", key, err.getMessage());
                    return loader.get();
                });
    }

    /** sha1 hex of the query — stable cache key for a given SQL. */
    public static String hash(String sql) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(sql.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(40);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            return Integer.toHexString(sql.hashCode());
        }
    }
}
