package com.sanedge.card.repository;

import java.time.Instant;
import java.util.List;

import com.sanedge.card.entity.CardPayment;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardPaymentRepository implements PanacheRepository<CardPayment> {

    @WithTransaction
    public Uni<List<CardPayment>> findByCardNumber(String cardNumber, int page, int size) {
        return find("cardNumber = ?1 AND deletedAt IS NULL ORDER BY createdAt DESC", cardNumber)
                .page(Page.of(page - 1, size))
                .list();
    }

    @WithTransaction
    public Uni<Long> countByCardNumber(String cardNumber) {
        return count("cardNumber = ?1 AND deletedAt IS NULL", cardNumber);
    }

    @WithTransaction
    public Uni<CardPayment> completePayment(Long paymentId) {
        return findById(paymentId)
                .chain(payment -> {
                    if (payment == null) {
                        return Uni.createFrom().nullItem();
                    }
                    payment.status = "COMPLETED";
                    payment.paidAt = Instant.now();
                    return persist(payment).map(v -> payment);
                });
    }
}
