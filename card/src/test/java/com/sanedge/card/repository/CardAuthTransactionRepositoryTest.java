package com.sanedge.card.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.sanedge.card.entity.CardAuthTransaction;
import com.sanedge.card.entity.CardAuthTransaction.AuthTxnStatus;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;

@QuarkusTest
@RunOnVertxContext
class CardAuthTransactionRepositoryTest {

    @Inject
    CardAuthTransactionRepository repository;

    private Uni<CardAuthTransaction> persistAuth(String cardNumber, String idempotencyKey, AuthTxnStatus status) {
        CardAuthTransaction txn = new CardAuthTransaction();
        txn.setCardNumber(cardNumber);
        txn.setIdempotencyKey(idempotencyKey);
        txn.setStatus(status);
        txn.setCreatedAt(Instant.now());
        txn.setUpdatedAt(Instant.now());
        return repository.persist(txn).map(t -> t);
    }

    private Uni<Void> clean() {
        return repository.deleteAll().replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByIdempotencyKey() {
        return clean()
                .chain(() -> persistAuth("CARD-AUTH", "key-1", AuthTxnStatus.PENDING))
                .chain(() -> repository.findByIdempotencyKey("key-1"))
                .invoke(found -> {
                    assertThat(found).isNotNull();
                    assertThat(found.getIdempotencyKey()).isEqualTo("key-1");
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testFindByCardNumber_Paginated() {
        return clean()
                .chain(() -> persistAuth("CARD-AUTH-2", "k1", AuthTxnStatus.PENDING))
                .chain(() -> persistAuth("CARD-AUTH-2", "k2", AuthTxnStatus.APPROVED))
                .chain(() -> repository.findByCardNumber("CARD-AUTH-2", 1, 10))
                .invoke(list -> assertThat(list).hasSize(2))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCountByCardNumber() {
        return clean()
                .chain(() -> persistAuth("CARD-COUNT", "k", AuthTxnStatus.PENDING))
                .chain(() -> repository.countByCardNumber("CARD-COUNT"))
                .invoke(count -> assertThat(count).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testCountRecentByCardNumber() {
        return clean()
                .chain(() -> persistAuth("CARD-RECENT", "k", AuthTxnStatus.PENDING))
                .chain(() -> {
                    Instant since = Instant.now().minus(1, ChronoUnit.DAYS);
                    return repository.countRecentByCardNumber("CARD-RECENT", since);
                })
                .invoke(count -> assertThat(count).isEqualTo(1))
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_Approve() {
        return clean()
                .chain(() -> persistAuth("CARD-STAT", "k", AuthTxnStatus.PENDING))
                .chain(txn -> repository.updateStatus(txn.getAuthTxnId(), AuthTxnStatus.APPROVED))
                .invoke(updated -> {
                    assertThat(updated.getStatus()).isEqualTo(AuthTxnStatus.APPROVED);
                    assertThat(updated.getAuthorizedAt()).isNotNull();
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_Reversed() {
        return clean()
                .chain(() -> persistAuth("CARD-REV", "k", AuthTxnStatus.PENDING))
                .chain(txn -> repository.updateStatus(txn.getAuthTxnId(), AuthTxnStatus.REVERSED))
                .invoke(updated -> assertThat(updated.getReversedAt()).isNotNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateStatus_NotFound() {
        return clean()
                .chain(() -> repository.updateStatus(99999L, AuthTxnStatus.APPROVED))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateRiskScore() {
        return clean()
                .chain(() -> persistAuth("CARD-RISK", "k", AuthTxnStatus.PENDING))
                .chain(txn -> repository.updateRiskScore(txn.getAuthTxnId(), 85))
                .invoke(updated -> {
                    assertThat(updated).isNotNull();
                    assertThat(updated.getRiskScore()).isEqualTo(85);
                })
                .replaceWithVoid();
    }

    @Test
    @WithTransaction
    Uni<Void> testUpdateRiskScore_NotFound() {
        return clean()
                .chain(() -> repository.updateRiskScore(99999L, 50))
                .invoke(updated -> assertThat(updated).isNull())
                .replaceWithVoid();
    }
}