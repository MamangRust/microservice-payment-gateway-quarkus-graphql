package com.sanedge.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaSecurityConfig (Phase 5 — unified security)")
class KafkaSecurityConfigTest {

    @Test
    void producer_setsDurabilityDefaults() {
        Map<String, String> config = KafkaSecurityConfig.producer("b:9092", "all", true);

        assertThat(config)
                .containsEntry("bootstrap.servers", "b:9092")
                .containsEntry("acks", "all")
                .containsEntry("enable.idempotence", "true")
                .containsEntry("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
                .containsEntry("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        assertThat(config).doesNotContainKey("security.protocol");
    }

    @Test
    void consumer_setsGroupAndDeserializers() {
        Map<String, String> config = KafkaSecurityConfig.consumer("b:9092", "email-service-group");

        assertThat(config)
                .containsEntry("group.id", "email-service-group")
                .containsEntry("auto.offset.reset", "earliest")
                .containsEntry("enable.auto.commit", "false")
                .containsEntry("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
                .containsEntry("value.deserializer", "io.vertx.kafka.client.serialization.JsonObjectDeserializer");
        assertThat(config).doesNotContainKey("security.protocol");
    }

    @Test
    void applySecurity_plaintextByDefault_isNoOp() {
        Map<String, String> config = new HashMap<>();
        KafkaSecurityConfig.applySecurity(config);
        assertThat(config).isEmpty();
    }

    @Test
    void applySecurity_saslSsl_appliesKeys() {
        Map<String, String> env = new HashMap<>();
        env.put("KAFKA_SECURITY_PROTOCOL", "SASL_SSL");
        env.put("KAFKA_SASL_MECHANISM", "SCRAM-SHA-256");
        env.put("KAFKA_SASL_JAAS_CONFIG",
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"app\" password=\"secret\";");
        env.put("KAFKA_SSL_TRUSTSTORE_LOCATION", "/certs/kafka.truststore.jks");
        env.put("KAFKA_SSL_TRUSTSTORE_PASSWORD", "trustpass");

        Map<String, String> config = new HashMap<>();
        KafkaSecurityConfig.applySecurity(config, env);

        assertThat(config)
                .containsEntry("security.protocol", "SASL_SSL")
                .containsEntry("sasl.mechanism", "SCRAM-SHA-256")
                .containsEntry("ssl.truststore.location", "/certs/kafka.truststore.jks")
                .containsEntry("ssl.truststore.password", "trustpass");
    }
}
