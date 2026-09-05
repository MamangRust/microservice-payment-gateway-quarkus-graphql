package com.sanedge.user.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Identity seeder: default roles (replaces the former V28 migration),
 * demo users with hashed passwords, and role assignments.
 *
 * <p>Idempotent: {@code ON CONFLICT (role_name|username)} / PK
 * {@code (user_id, role_id)} DO NOTHING. Domain {@code identity} (order 10)
 * must run before merchant/card seeders.</p>
 */
public class UserSeeder implements Seeder {

    @Override
    public String domain() {
        return "identity";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();
        var hash = ctx.passwordHasher();

        // 1. Roles (reference data, formerly V28__Seed_default_roles.sql)
        Uni<Void> roles = pg.preparedQuery(
                "INSERT INTO payment_identity.roles (role_name) VALUES ($1), ($2), ($3) "
                        + "ON CONFLICT (role_name) DO NOTHING")
                .execute(Tuple.of("ROLE_ADMIN", "ROLE_STAFF", "ROLE_USER"))
                .replaceWithVoid();

        // 2. Users (password hashed at runtime via PasswordUtil)
        String userSql = "INSERT INTO payment_identity.users (firstname, lastname, username, email, password) "
                + "VALUES ($1, $2, $3, $4, $5) ON CONFLICT (username) DO NOTHING";
        Uni<Void> users = pg.preparedQuery(userSql).execute(Tuple.of("Admin", "Admin", "admin", "admin@example.com", hash.apply("admin123")))
                .chain(r -> pg.preparedQuery(userSql).execute(Tuple.of("Staff", "Staff", "staff", "staff@example.com", hash.apply("staff123"))))
                .chain(r -> pg.preparedQuery(userSql).execute(Tuple.of("User", "User", "user", "user@example.com", hash.apply("user123"))))
                .replaceWithVoid();

        // 3. Role assignments
        String roleSql = "INSERT INTO payment_identity.user_roles (user_id, role_id) "
                + "SELECT u.id, r.id FROM payment_identity.users u, payment_identity.roles r "
                + "WHERE u.username = $1 AND r.role_name = $2 "
                + "ON CONFLICT (user_id, role_id) DO NOTHING";
        Uni<Void> userRoles = pg.preparedQuery(roleSql).execute(Tuple.of("admin", "ROLE_ADMIN"))
                .chain(r -> pg.preparedQuery(roleSql).execute(Tuple.of("staff", "ROLE_STAFF")))
                .chain(r -> pg.preparedQuery(roleSql).execute(Tuple.of("user", "ROLE_USER")))
                .replaceWithVoid();

        return roles.chain(v -> users).chain(v -> userRoles);
    }
}
