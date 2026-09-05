package com.sanedge.card.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sanedge.common.test.PostgreSqlResource;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

/**
 * Executable proof that the card event log sink is idempotent under
 * at-least-once Kafka redelivery:
 * <ul>
 * <li>the same (topic, reference_id) is appended only once;</li>
 * <li>events without a stable reference (card.statement.generated) are always
 * appended;</li>
 * <li>distinct references on the same topic are both kept.</li>
 * </ul>
 */
@QuarkusTestResource(PostgreSqlResource.class)
@QuarkusTest
@RunOnVertxContext
class CardEventLogRepositoryTest {

    @Inject
    CardEventLogRepository eventLogRepo;

    private Uni<Void> clean() {
        return Panache.withTransaction(() -> eventLogRepo.deleteAll().replaceWithVoid());
    }

    @Test
    void appendIfAbsent_deduplicatesSameTopicReference(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> eventLogRepo.appendIfAbsent("card.payment.posted", "card.payment.posted",
                        "4111111111111111", "42", "{\"paymentId\":42}"))
                .chain(() -> eventLogRepo.appendIfAbsent("card.payment.posted", "card.payment.posted",
                        "4111111111111111", "42", "{\"paymentId\":42}"))
                .chain(() -> Panache.withSession(
                        () -> eventLogRepo.count("topic = ?1 AND referenceId = ?2", "card.payment.posted", "42")))
                .invoke(count -> assertThat(count).isEqualTo(1L))
                .replaceWithVoid());
    }

    @Test
    void appendIfAbsent_alwaysAppendsWhenReferenceNull(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> eventLogRepo.appendIfAbsent("card.statement.generated", "card.statement.generated",
                        null, null, "{\"billingCycleDay\":10,\"statementsProcessed\":3}"))
                .chain(() -> eventLogRepo.appendIfAbsent("card.statement.generated", "card.statement.generated",
                        null, null, "{\"billingCycleDay\":10,\"statementsProcessed\":3}"))
                .chain(() -> Panache.withSession(() -> eventLogRepo.count("topic = ?1", "card.statement.generated")))
                .invoke(count -> assertThat(count).isEqualTo(2L))
                .replaceWithVoid());
    }

    @Test
    void appendIfAbsent_keepsDistinctReferencesOnSameTopic(UniAsserter asserter) {
        asserter.execute(() -> clean()
                .chain(() -> eventLogRepo.appendIfAbsent("card.fraud.alert", "card.fraud.alert",
                        "4111111111111111", "100", "{\"authTxnId\":100}"))
                .chain(() -> eventLogRepo.appendIfAbsent("card.fraud.alert", "card.fraud.alert",
                        "4111111111111111", "101", "{\"authTxnId\":101}"))
                .chain(() -> Panache.withSession(() -> eventLogRepo.count("topic = ?1", "card.fraud.alert")))
                .invoke(count -> assertThat(count).isEqualTo(2L))
                .replaceWithVoid());
    }
}
