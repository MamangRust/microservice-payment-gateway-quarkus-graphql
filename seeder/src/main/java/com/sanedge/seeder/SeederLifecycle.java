package com.sanedge.seeder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.logging.Logger;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;
import com.sanedge.common.utils.PasswordUtil;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Pool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * One-shot seeder orchestrator (Quarkus equivalent of the Go seed runner).
 *
 * <p>On startup it discovers every {@link Seeder} registered via
 * {@code META-INF/services/com.sanedge.common.seed.Seeder}, sorts them by
 * {@link Seeder#order()}, optionally filters by {@code SEED_DOMAINS}
 * (comma-separated, e.g. {@code identity,merchant}), and runs them
 * sequentially against the payment gateway database. The process exits with
 * code 0 on success and 1 on any failure.</p>
 *
 * <p>Usage:
 * {@code SEED_DOMAINS=identity,merchant java -jar seeder/target/quarkus-app/quarkus-run.jar}</p>
 */
@ApplicationScoped
public class SeederLifecycle {

    private static final Logger LOGGER = Logger.getLogger(SeederLifecycle.class);

    @Inject
    Pool pool;

    void onStart(@Observes StartupEvent ev) {
        List<Seeder> seeders = new ArrayList<>();
        ServiceLoader.load(Seeder.class).forEach(seeders::add);
        seeders.sort(Comparator.comparingInt(Seeder::order));

        List<String> filter = parseSeedDomains(System.getenv("SEED_DOMAINS"));

        LOGGER.infof("Seeder orchestrator started. Discovered %d seeder(s): %s",
                seeders.size(), seeders.stream().map(Seeder::domain).toList());
        if (!filter.isEmpty()) {
            LOGGER.infof("SEED_DOMAINS filter active: %s", filter);
        }

        SeedContext ctx = new SeedContext(pool, LOGGER, new PasswordUtil()::hashPassword);

        Uni<Void> chain = Uni.createFrom().voidItem();
        for (Seeder seeder : seeders) {
            if (!filter.isEmpty() && !filter.contains(seeder.domain())) {
                LOGGER.infof("Skipping domain '%s' (not in SEED_DOMAINS)", seeder.domain());
                continue;
            }
            chain = chain
                    .invoke(() -> LOGGER.infof("Seeding domain '%s' ...", seeder.domain()))
                    .chain(v -> seeder.seed(ctx))
                    .invoke(() -> LOGGER.infof("Domain '%s' seeded.", seeder.domain()));
        }

        chain.subscribe().with(
                v -> {
                    LOGGER.info("Seeding completed successfully. Shutting down seeder runner.");
                    Quarkus.asyncExit(0);
                },
                failure -> {
                    LOGGER.errorf(failure, "Seeding failed: %s", failure.getMessage());
                    Quarkus.asyncExit(1);
                });
    }

    private static List<String> parseSeedDomains(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<String> domains = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                domains.add(trimmed);
            }
        }
        return domains;
    }
}
