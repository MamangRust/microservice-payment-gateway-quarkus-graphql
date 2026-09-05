package com.sanedge.role.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.sanedge.common.domain.response.PagedResult;
import com.sanedge.role.entity.Role;
import com.sanedge.role.domain.requests.FindAllRoles;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RoleRepository implements PanacheRepository<Role> {

    public Uni<PagedResult<Role>> findRoles(FindAllRoles req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    (?1 IS NULL
                     OR LOWER(roleName) LIKE LOWER(CONCAT('%', ?1, '%')))
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Role>> findActiveRoles(FindAllRoles req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(roleName) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<PagedResult<Role>> findTrashedRoles(FindAllRoles req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int size = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : null;

        var query = """
                    deletedAt IS NOT NULL
                    AND (
                        ?1 IS NULL
                        OR LOWER(roleName) LIKE LOWER(CONCAT('%', ?1, '%'))
                    )
                """;

        var panacheQuery = find(query, keyword)
                .page(page, size);

        return Uni.combine().all().unis(panacheQuery.list(), panacheQuery.count())
                .asTuple()
                .map(tuple -> new PagedResult<>(tuple.getItem1(), tuple.getItem2().intValue()));
    }

    public Uni<Role> findByRoleName(String roleName) {
        return find("roleName", roleName).firstResult();
    }

    public Uni<List<Role>> findUserRoles(Long userId) {
        return find("""
                    SELECT ur.role
                    FROM UserRole ur
                    WHERE ur.userId = ?1
                    ORDER BY ur.role.createdAt ASC
                """, userId).list();
    }

    @WithTransaction
    public Uni<Role> trash(Long roleId) {
        return findById(roleId)
                .chain(role -> {
                    if (role != null) {
                        LocalDateTime date = LocalDateTime.now();
                        role.setDeletedAt(Timestamp.valueOf(date));
                        return persist(role).map(v -> role);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Role> restore(Long roleId) {
        return find("roleId = ?1 AND deletedAt IS NOT NULL", roleId).firstResult()
                .chain(role -> {
                    if (role != null) {
                        role.setDeletedAt(null);
                        return persist(role).map(v -> role);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Role> deletePermanent(Long roleId) {
        return findById(roleId)
                .chain(role -> {
                    if (role != null) {
                        return delete(role).map(v -> role);
                    }
                    return Uni.createFrom().nullItem();
                });
    }

    @WithTransaction
    public Uni<Boolean> restoreAllDeleted() {
        return update("deletedAt = NULL WHERE deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }

    @WithTransaction
    public Uni<Boolean> deleteAllDeleted() {
        return delete("deletedAt IS NOT NULL")
                .map(count -> count > 0);
    }
}
