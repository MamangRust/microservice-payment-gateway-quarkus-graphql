package com.sanedge.card.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.sanedge.card.entity.CardPayment;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class CardPaymentRepositoryTest {

    @Inject
    CardPaymentRepository repository;

    private Uni<CardPayment> persistPayment(String cardNumber, String status) {
        CardPayment payment = new CardPayment();
        payment.setCardNumber(cardNumber);
        payment.setStatus(status);
        payment.setCreatedAt(Instant.now());
        payment.setUpdatedAt(Instant.now());
        return repository.persist(payment).map(p -> p);
    }

    private Uni<Void> clean() {
        return repository.deleteAll().replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumber() {
        return clean()
                .chain(() -> persistPayment("CARD-PAY", "PENDING"))
                .chain(() -> persistPayment("CARD-PAY", "COMPLETED"))
                .chain(() -> repository.findByCardNumber("CARD-PAY", 1, 10))
                .invoke(list -> {
                    assertThat(list).hasSize(2);
                    // descending by createdAt, so newest first
                    assertThat(list.get(0).getStatus()).isEqualTo("COMPLETED");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCountByCardNumber() {
        return clean()
                .chain(() -> persistPayment("CARD-COUNT", "PENDING"))
                .chain(() -> repository.countByCardNumber("CARD-COUNT"))
                .invoke(count -> assertThat(count).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCompletePayment() {
        return clean()
                .chain(() -> persistPayment("CARD-COMP", "PENDING"))
                .chain(payment -> repository.completePayment(payment.getPaymentId()))
                .invoke(completed -> {
                    assertThat(completed).isNotNull();
                    assertThat(completed.getStatus()).isEqualTo("COMPLETED");
                    assertThat(completed.getPaidAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCompletePayment_NotFound() {
        return clean()
                .chain(() -> repository.completePayment(99999L))
                .invoke(completed -> assertThat(completed).isNull())
                .replaceWithVoid();
    }
}