package com.sanedge.card.repository;

import java.sql.Date;
import java.util.List;

import com.sanedge.card.entity.BillingStatement;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BillingStatementRepository implements PanacheRepository<BillingStatement> {

    @WithTransaction
    public Uni<BillingStatement> findByCardNumberAndStatementDate(String cardNumber, Date statementDate) {
        return find("cardNumber = ?1 AND statementDate = ?2 AND deletedAt IS NULL", cardNumber, statementDate)
                .firstResult();
    }

    @WithTransaction
    public Uni<List<BillingStatement>> findByCardNumber(String cardNumber, int page, int size) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber)
                .page(Page.of(page - 1, size))
                .list();
    }

    @WithTransaction
    public Uni<Long> countByCardNumber(String cardNumber) {
        return count("cardNumber = ?1 AND deletedAt IS NULL", cardNumber);
    }

    @WithTransaction
    public Uni<List<BillingStatement>> findByBillingCycleDay(int billingCycleDay) {
        return find("billingCycleDay = ?1 AND deletedAt IS NULL AND status = 'OPEN'", billingCycleDay).list();
    }
}
