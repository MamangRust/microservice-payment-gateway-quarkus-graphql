package com.sanedge.common.seed;

import io.smallrye.mutiny.Uni;

/**
 * SPI for per-domain database seeders.
 *
 * <p>Every domain module that needs fixture/reference data implements this
 * interface (plain class, public no-arg constructor) and registers it via
 * {@code META-INF/services/com.sanedge.common.seed.Seeder} so the
 * {@code seeder} orchestrator module can discover it with
 * {@link java.util.ServiceLoader}.</p>
 *
 * <p>Seeders must be idempotent (re-runnable without duplicates) and use
 * schema-qualified native SQL through the {@link SeedContext} pool.</p>
 */
public interface Seeder {

    /**
     * Domain name used for selective seeding via {@code SEED_DOMAINS}
     * (e.g. {@code identity}, {@code merchant}, {@code card}, {@code saldo},
     * {@code topup}, {@code transaction}, {@code transfer}, {@code withdraw}).
     */
    String domain();

    /**
     * Execution order across domains; lower runs first. Identity (10) must run
     * before merchant/card (20), then saldo (30), then finance (40).
     */
    default int order() {
        return 100;
    }

    Uni<Void> seed(SeedContext ctx);
}
