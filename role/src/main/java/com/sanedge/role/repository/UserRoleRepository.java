package com.sanedge.role.repository;

import com.sanedge.role.entity.UserRole;
import com.sanedge.role.entity.UserRoleId;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRoleRepository implements PanacheRepositoryBase<UserRole, UserRoleId> {

    @WithTransaction
    public Uni<UserRole> assignRole(Long userId, Long roleId, RoleRepository roleRepository) {
        return roleRepository.findById(roleId)
                .chain(role -> {
                    if (role == null) {
                        return Uni.createFrom().failure(new jakarta.ws.rs.NotFoundException("Role not found with id: " + roleId));
                    }
                    UserRole userRole = new UserRole(userId, role);
                    return persist(userRole);
                });
    }

    @WithTransaction
    public Uni<Boolean> removeRole(Long userId, Long roleId) {
        return delete("userId = ?1 and role.id = ?2", userId, roleId)
                .map(count -> count > 0);
    }
}
