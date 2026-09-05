package com.sanedge.transfer.repository;

import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.transfer.entity.Transfer;
import com.sanedge.transfer.domain.requests.FindAllTransfers;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TransferQueryRepository implements PanacheRepository<Transfer> {

    public Uni<PagedResult<Transfer>> findTransfers(FindAllTransfers req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(transferFrom) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(transferTo) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transferTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Transfer>> findActiveTransfers(FindAllTransfers req) {
        return findTransfers(req);
    }

    public Uni<PagedResult<Transfer>> findTrashedTransfers(FindAllTransfers req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String search = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(transferFrom) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(transferTo) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(CAST(status AS string)) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("transferTime"), search)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Transfer> findTransferById(Long id) {
        return find("transferId = ?1 AND deletedAt IS NULL", id).firstResult();
    }

    public Uni<List<Transfer>> findTransfersByCardNumber(String cardNumber) {
        return list("deletedAt IS NULL AND (transferFrom = ?1 OR transferTo = ?1)", Sort.descending("transferTime"), cardNumber);
    }

    public Uni<List<Transfer>> findTransfersBySourceCard(String cardNumber) {
        return list("deletedAt IS NULL AND transferFrom = ?1", Sort.descending("transferTime"), cardNumber);
    }

    public Uni<List<Transfer>> findTransfersByDestinationCard(String cardNumber) {
        return list("deletedAt IS NULL AND transferTo = ?1", Sort.descending("transferTime"), cardNumber);
    }

    public Uni<Void> lockIdempotencyKey(String key) {
        return getSession()
                .chain(session -> session.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:key))", Object.class)
                        .setParameter("key", key)
                        .getSingleResult())
                .replaceWithVoid();
    }

    public Uni<Transfer> findByIdempotencyKey(String key) {
        return find("idempotencyKey = ?1", key).firstResult();
    }

    public Uni<java.util.List<Transfer>> findPendingCompensation(int maxAttempts) {
        return list("status = ?1 AND deletedAt IS NULL AND compensationAttempts < ?2",
                com.sanedge.common.enums.Status.COMPENSATION_REQUIRED, maxAttempts);
    }
}
