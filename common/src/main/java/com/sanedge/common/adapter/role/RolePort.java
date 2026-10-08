package com.sanedge.common.adapter.role;

import com.sanedge.common.adapter.model.Role;

import io.smallrye.mutiny.Uni;

/**
 * Read-only role surface. Role assignment lives in {@link UserRolePort}.
 */
public interface RolePort {

    Uni<Role> findById(int roleId);

    Uni<Role> findByName(String name);
}
