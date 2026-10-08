package com.sanedge.common.adapter.role;

import java.util.List;

import com.sanedge.common.adapter.model.Role;
import com.sanedge.common.adapter.model.UserRole;

import io.smallrye.mutiny.Uni;

/**
 * User&harr;role surface: resolving a user's roles and (un)assigning them. Owns
 * the calls to the role service's user-role RPCs.
 */
public interface UserRolePort {

    Uni<List<Role>> findByUserId(int userId);

    Uni<UserRole> assignRoleToUser(int userId, int roleId);

    Uni<Void> removeRoleFromUser(int userId, int roleId);
}
