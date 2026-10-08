package com.sanedge.common.adapter.model;

/**
 * Domain view of a user&harr;role assignment owned by the role service.
 */
public record UserRole(int userRoleId, int userId, int roleId) {
}
