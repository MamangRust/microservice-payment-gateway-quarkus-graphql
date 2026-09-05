package com.sanedge.common.test;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RedisResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("redis:7-alpine");
    private static final int REDIS_PORT = 6379;

    private GenericContainer<?> redisContainer;

    @Override
    public Map<String, String> start() {
        redisContainer = new GenericContainer<>(IMAGE_NAME)
                .withExposedPorts(REDIS_PORT);
        redisContainer.start();

        Map<String, String> properties = new HashMap<>();
        String host = redisContainer.getHost();
        Integer port = redisContainer.getMappedPort(REDIS_PORT);

        properties.put("quarkus.redis.hosts", "redis://" + host + ":" + port);

        return properties;
    }

    @Override
    public void stop() {
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }
}
