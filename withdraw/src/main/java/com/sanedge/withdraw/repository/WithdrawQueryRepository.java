package com.sanedge.withdraw.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.withdraw.entity.Withdraw;
import com.sanedge.withdraw.domain.requests.FindAllWithdraws;
import com.sanedge.withdraw.domain.requests.FindAllWithdrawCardNumber;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WithdrawQueryRepository implements PanacheRepository<Withdraw> {

    public Uni<PagedResult<Withdraw>> findAllWithdraws(FindAllWithdraws req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR cardNumber LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Withdraw>> findActiveWithdraws(FindAllWithdraws req) {
        return findAllWithdraws(req);
    }

    public Uni<PagedResult<Withdraw>> findTrashedWithdraws(FindAllWithdraws req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR cardNumber LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?1, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?1, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<List<Withdraw>> findByCardNumber(String cardNumber) {
        return list("deletedAt IS NULL AND cardNumber = ?1", Sort.descending("withdrawTime"), cardNumber);
    }

    public Uni<Void> lockIdempotencyKey(String key) {
        return getSession()
                .chain(session -> session.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))", Object.class)
                        .setParameter("key", key)
                        .getSingleResult())
                .replaceWithVoid();
    }

    public Uni<Withdraw> findByIdempotencyKey(String key) {
        return find("idempotencyKey = ?1", key).firstResult();
    }

    public Uni<java.util.List<Withdraw>> findPendingCompensation(int maxAttempts) {
        return list("status = ?1 AND deletedAt IS NULL AND compensationAttempts < ?2",
                com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts);
    }

    public Uni<PagedResult<Withdraw>> findAllByCardNumber(FindAllWithdrawCardNumber req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;
        String cardNumber = req.getCardNumber();

        var query = """
                    deletedAt IS NULL
                    AND cardNumber = ?1
                    AND (
                        ?2 IS NULL
                        OR CAST(withdrawAmount AS string) LIKE CONCAT('%', ?2, '%')
                        OR CAST(withdrawTime AS string) LIKE CONCAT('%', ?2, '%')
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?2, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("withdrawTime"), cardNumber, search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
