package com.sanedge.saldo.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.saldo.entity.Saldo;
import com.sanedge.saldo.domain.requests.FindAllSaldos;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SaldoQueryRepository implements PanacheRepository<Saldo> {

    public Uni<PagedResult<Saldo>> findSaldos(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (?1 IS NULL OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%')))
                """;

        var panacheQuery = find(query, Sort.ascending("saldoId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Saldo> findByCardNumber(String cardNumber) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber).firstResult();
    }

    public Uni<PagedResult<Saldo>> findActiveSaldos(FindAllSaldos req) {
        return findSaldos(req);
    }

    public Uni<PagedResult<Saldo>> findTrashedSaldos(FindAllSaldos req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (?1 IS NULL OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%')))
                """;

        var panacheQuery = find(query, Sort.ascending("saldoId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}
