package com.sanedge.user.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.sanedge.user.domain.requests.FindAllUsers;
import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.user.entity.User;

import io.quarkus.hibernate.reactive.panache.PanacheQuery;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    private PanacheQuery<User> buildSearchQuery(String deletedAtClause, String keyword, int page, int size) {
        StringBuilder sb = new StringBuilder("FROM User u LEFT JOIN FETCH u.roles WHERE ").append(deletedAtClause);
        if (keyword != null) {
            sb.append(" AND (LOWER(u.firstname) LIKE LOWER(CONCAT('%', ?1, '%'))")
              .append(" OR LOWER(u.lastname)  LIKE LOWER(CONCAT('%', ?1, '%'))")
              .append(" OR LOWER(u.email)     LIKE LOWER(CONCAT('%', ?1, '%')))");
            sb.append(" ORDER BY u.id ASC");
            return find(sb.toString(), keyword).page(page, size);
        } else {
            sb.append(" ORDER BY u.id ASC");
            return find(sb.toString()).page(page, size);
        }
    }

    private Uni<PagedResult<User>> pageResult(PanacheQuery<User> panacheQuery) {
        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    private int normalizePage(int page) {
        return page > 0 ? page - 1 : 0;
    }

    private int normalizeSize(int size) {
        return size > 0 ? size : 10;
    }

    private String normalizeKeyword(String search) {
        return (search != null && !search.isEmpty()) ? search : null;
    }

    public Uni<PagedResult<User>> findUsers(FindAllUsers req) {
        int page = normalizePage(req.getPage());
        int size = normalizeSize(req.getPageSize());
        String keyword = normalizeKeyword(req.getSearch());
        return pageResult(buildSearchQuery("u.deletedAt IS NULL", keyword, page, size));
    }

    public Uni<PagedResult<User>> findActiveUsers(FindAllUsers req) {
        int page = normalizePage(req.getPage());
        int size = normalizeSize(req.getPageSize());
        String keyword = normalizeKeyword(req.getSearch());
        return pageResult(buildSearchQuery("u.deletedAt IS NULL", keyword, page, size));
    }

    public Uni<PagedResult<User>> findTrashedUsers(FindAllUsers req) {
        int page = normalizePage(req.getPage());
        int size = normalizeSize(req.getPageSize());
        String keyword = normalizeKeyword(req.getSearch());
        return pageResult(buildSearchQuery("u.deletedAt IS NOT NULL", keyword, page, size));
    }

    public Uni<User> findById(Integer id) {
        return find("id", Long.valueOf(id)).firstResult();
    }

    public Uni<User> findByUsername(String username) {
        return find("username", username).firstResult();
    }

    public Uni<User> findByEmail(String email) {
        return find("email", email).firstResult();
    }

    public Uni<Boolean> existsByUsername(String username) {
        return count("username = ?1", username).map(c -> c > 0);
    }

    public Uni<Boolean> existsByEmail(String email) {
        return count("email = ?1", email).map(c -> c > 0);
    }

    @WithTransaction
    public Uni<User> trash(Long userId) {
        return findById(userId)
                .chain(user -> {
                    if (user != null && user.getDeletedAt() == null) {
                        LocalDateTime date = LocalDateTime.now();
                        user.setDeletedAt(Timestamp.valueOf(date));
                        return persist(user).map(v -> user);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<User> restore(Long userId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", userId).firstResult()
                .chain(user -> {
                    if (user != null) {
                        user.setDeletedAt(null);
                        return persist(user).map(v -> user);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<User> deletePermanent(Long userId) {
        return find("id = ?1 AND deletedAt IS NOT NULL", userId).firstResult()
                .chain(user -> {
                    if (user != null) {
                        return delete(user).map(v -> user);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(updatedCount -> updatedCount > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(deletedCount -> deletedCount > 0);
    }
}
