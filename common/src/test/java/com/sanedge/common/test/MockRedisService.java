package com.sanedge.common.test;

import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import com.sanedge.common.config.RedisService;

@Mock
@Alternative
@ApplicationScoped
public class MockRedisService extends RedisService {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    public MockRedisService() {
        super();
    }

    @Override
    public void set(String key, String value) {
        store.put(key, value);
    }

    @Override
    public Uni<Void> setWithExpirationReactive(String key, String value, long seconds) {
        store.put(key, value);
        return Uni.createFrom().voidItem();
    }

    @Override
    public String get(String key) {
        return store.get(key);
    }

    @Override
    public Uni<String> getReactive(String key) {
        return Uni.createFrom().item(store.get(key));
    }

    @Override
    public Uni<Void> setReactive(String key, String value) {
        store.put(key, value);
        return Uni.createFrom().voidItem();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public Uni<Void> deleteReactive(String key) {
        store.remove(key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public boolean exists(String key) {
        return store.containsKey(key);
    }

    @Override
    public Uni<Boolean> existsReactive(String key) {
        return Uni.createFrom().item(store.containsKey(key));
    }

    public void clear() {
        store.clear();
    }
}
