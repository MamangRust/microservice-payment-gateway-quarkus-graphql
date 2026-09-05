package com.sanedge.card.repository;

import java.util.Optional;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.card.entity.Card;
import com.sanedge.card.domain.requests.FindAllCards;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CardQueryRepository implements PanacheRepository<Card> {

    public Uni<Optional<Card>> findCardById(Long cardId) {
        return find("cardId = ?1 AND deletedAt IS NULL", cardId)
                .firstResult()
                .map(Optional::ofNullable);
    }

    public Uni<Optional<Card>> findCardByUserId(Long userId) {
        if (userId == null) {
            return Uni.createFrom().item(Optional.empty());
        }
        return find("userId = ?1 AND deletedAt IS NULL", userId)
                .firstResult()
                .map(Optional::ofNullable);
    }

    public Uni<Optional<Card>> findCardByCardNumber(String cardNumber) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber)
                .firstResult()
                .map(Optional::ofNullable);
    }

    public Uni<Card> findByCardNumber(String cardNumber) {
        return find("cardNumber = ?1 AND deletedAt IS NULL", cardNumber).firstResult();
    }

    public Uni<PagedResult<Card>> findCards(FindAllCards req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(cardType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(cardProvider) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Card>> findActiveCards(FindAllCards req) {
        return findCards(req);
    }

    public Uni<PagedResult<Card>> findTrashedCards(FindAllCards req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(cardNumber) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(cardType) LIKE LOWER(CONCAT('%', ?1, '%'))
                        OR LOWER(cardProvider) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }
}