package com.sanedge.card.test;

import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import com.sanedge.card.service.KafkaService;

/**
 * Mock CDI alternative for {@link KafkaService}.
 * <p>
 * All Kafka send operations are no-ops, so tests don't need a real Kafka broker.
 */
@Mock
@Alternative
@ApplicationScoped
public class MockKafkaService extends KafkaService {

    @Override
    public Uni<Void> sendMessage(String topic, String key, JsonObject payload) {
        return Uni.createFrom().voidItem();
    }
}
