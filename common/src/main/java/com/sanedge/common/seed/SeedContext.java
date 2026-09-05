package com.sanedge.common.seed;

import java.util.function.Function;

import org.jboss.logging.Logger;

import io.vertx.mutiny.sqlclient.Pool;

/**
 * Dependencies handed to every {@link Seeder} by the {@code seeder} module
 * (Quarkus equivalent of the Go {@code Deps{Db, Ctx, Logger, Hash}}).
 *
 * @param pool           reactive SQL pool bound to the payment gateway database
 * @param log            JBoss logger for progress/error reporting
 * @param passwordHasher function that produces a salted password hash
 *                       (e.g. {@code new PasswordUtil()::hashPassword})
 */
public record SeedContext(Pool pool, Logger log, Function<String, String> passwordHasher) {
}
