package com.sanedge.merchant.repository;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.merchant.entity.MerchantDocument;
import com.sanedge.merchant.domain.requests.FindAllMerchantDocuments;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MerchantDocumentQueryRepository implements PanacheRepository<MerchantDocument> {

    public Uni<PagedResult<MerchantDocument>> findDocuments(FindAllMerchantDocuments req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    ?1 IS NULL
                    OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                    OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                """;

        var panacheQuery = find(query, Sort.ascending("documentId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantDocument>> findActiveDocuments(FindAllMerchantDocuments req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.ascending("documentId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<MerchantDocument>> findTrashedDocuments(FindAllMerchantDocuments req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(documentType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(documentUrl) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(status) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(note) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, Sort.descending("documentId"), keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<MerchantDocument> findDocumentById(Long documentId) {
        return find("documentId = ?1 AND deletedAt IS NULL", documentId).firstResult();
    }
}
