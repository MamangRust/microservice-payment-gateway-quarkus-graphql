package com.sanedge.common.config;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisService {
    @Inject
    RedisDataSource redisDataSource;

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    private ValueCommands<String, String> valueCommands;
    private ReactiveValueCommands<String, String> reactiveValueCommands;
    private ReactiveKeyCommands<String> reactiveKeyCommands;

    public RedisService() {
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        this.valueCommands = redisDataSource.value(String.class, String.class);
        this.reactiveValueCommands = reactiveRedisDataSource.value(String.class, String.class);
        this.reactiveKeyCommands = reactiveRedisDataSource.key(String.class);
    }

    public void set(String key, String value) {
        valueCommands.set(key, value);
    }

    public Uni<Void> setWithExpirationReactive(String key, String value, long seconds) {
        return reactiveValueCommands.setex(key, seconds, value);
    }

    public String get(String key) {
        return valueCommands.get(key);
    }

    public Uni<String> getReactive(String key) {
        return reactiveValueCommands.get(key);
    }

    public Uni<Void> setReactive(String key, String value) {
        return reactiveValueCommands.set(key, value);
    }

    public void delete(String key) {
        redisDataSource.key().del(key);
    }

    public Uni<Void> deleteReactive(String key) {
        return reactiveKeyCommands.del(key)
                .replaceWithVoid();
    }

    public boolean exists(String key) {
        return redisDataSource.key().exists(key);
    }

    public Uni<Boolean> existsReactive(String key) {
        return reactiveKeyCommands.exists(key);
    }
}
