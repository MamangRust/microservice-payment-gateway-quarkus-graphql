package com.sanedge.common.test;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class PostgreSqlResource implements QuarkusTestResourceLifecycleManager {

    private static final DockerImageName IMAGE_NAME = DockerImageName.parse("postgres:16-alpine");

    private PostgreSQLContainer<?> postgresContainer;

    @Override
    public Map<String, String> start() {
        postgresContainer = new PostgreSQLContainer<>(IMAGE_NAME)
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test");
        postgresContainer.start();

        Map<String, String> properties = new HashMap<>();
        String jdbcUrl = postgresContainer.getJdbcUrl();
        String reactiveUrl = jdbcUrl.replace("jdbc:postgresql://", "postgresql://");

        properties.put("quarkus.datasource.reactive.url", reactiveUrl);
        properties.put("quarkus.datasource.jdbc.url", jdbcUrl);
        properties.put("quarkus.datasource.username", postgresContainer.getUsername());
        properties.put("quarkus.datasource.password", postgresContainer.getPassword());

        // Ensure Hibernate ORM updates the schema
        properties.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        return properties;
    }

    @Override
    public void stop() {
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
    }
}
