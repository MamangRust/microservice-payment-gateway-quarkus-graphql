package com.sanedge.email;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka client config builder local to the email worker (Phase 5 — unified
 * security across the three email services).
 *
 * <p>Reads the same {@code KAFKA_*} env vars as the other services:
 * {@code KAFKA_SECURITY_PROTOCOL}, {@code KAFKA_SASL_MECHANISM},
 * {@code KAFKA_SASL_JAAS_CONFIG}, {@code KAFKA_SSL_TRUSTSTORE_LOCATION/PASSWORD/TYPE},
 * {@code KAFKA_SSL_KEYSTORE_LOCATION/PASSWORD}, {@code KAFKA_SSL_KEY_PASSWORD}.
 * Plaintext (no env vars) stays the local-dev default.
 */
final class KafkaSecurityConfig {

    static final String DEFAULT_ACKS = "all";

    private KafkaSecurityConfig() {
    }

    /**
     * Producer config: String key/value serializers + {@code acks=all} +
     * {@code enable.idempotence=true} (env-overridable) + optional security.
     */
    static Map<String, String> producer(String bootstrapServers, String acks, boolean idempotence) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("acks", acks);
        config.put("enable.idempotence", String.valueOf(idempotence));
        applySecurity(config);
        return config;
    }

    /**
     * Consumer config: String key / JsonObject value deserializers + group id
     * + earliest offset reset + manual commits + optional security.
     */
    static Map<String, String> consumer(String bootstrapServers, String groupId) {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "false");
        applySecurity(config);
        return config;
    }

    /** Applies SASL/SCRAM + TLS keys when {@code KAFKA_SECURITY_PROTOCOL} is set. */
    static void applySecurity(Map<String, String> config) {
        applySecurity(config, System.getenv());
    }

    /** Overload with explicit env source (used by unit tests). */
    static void applySecurity(Map<String, String> config, Map<String, String> env) {
        String protocol = env(env, "KAFKA_SECURITY_PROTOCOL", "PLAINTEXT");
        if (protocol.isBlank() || "PLAINTEXT".equalsIgnoreCase(protocol)) {
            return;
        }
        config.put("security.protocol", protocol);
        if (protocol.toUpperCase().contains("SASL")) {
            config.put("sasl.mechanism", env(env, "KAFKA_SASL_MECHANISM", "SCRAM-SHA-256"));
            String jaas = env(env, "KAFKA_SASL_JAAS_CONFIG", "");
            if (!jaas.isBlank()) {
                config.put("sasl.jaas.config", jaas);
            }
        }
        if (protocol.toUpperCase().contains("SSL")) {
            putIfNotBlank(config, "ssl.truststore.location", env(env, "KAFKA_SSL_TRUSTSTORE_LOCATION", ""));
            putIfNotBlank(config, "ssl.truststore.password", env(env, "KAFKA_SSL_TRUSTSTORE_PASSWORD", ""));
            putIfNotBlank(config, "ssl.truststore.type", env(env, "KAFKA_SSL_TRUSTSTORE_TYPE", "JKS"));
            putIfNotBlank(config, "ssl.keystore.location", env(env, "KAFKA_SSL_KEYSTORE_LOCATION", ""));
            putIfNotBlank(config, "ssl.keystore.password", env(env, "KAFKA_SSL_KEYSTORE_PASSWORD", ""));
            putIfNotBlank(config, "ssl.key.password", env(env, "KAFKA_SSL_KEY_PASSWORD", ""));
        }
    }

    private static String env(Map<String, String> env, String name, String defaultValue) {
        String value = env.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static void putIfNotBlank(Map<String, String> config, String key, String value) {
        if (value != null && !value.isBlank()) {
            config.put(key, value);
        }
    }
}
