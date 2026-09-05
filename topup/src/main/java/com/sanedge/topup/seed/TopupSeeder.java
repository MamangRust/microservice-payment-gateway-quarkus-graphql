package com.sanedge.topup.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Topup seeder: demo topups for the seeded cards.
 *
 * <p>Idempotent via the partial unique index on {@code idempotency_key}
 * ({@code idx_topups_idempotency_key}). Requires the card seeder to have run
 * first. Domain {@code topup} (order 40).</p>
 */
public class TopupSeeder implements Seeder {

    @Override
    public String domain() {
        return "topup";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_finance.topups "
                + "(idempotency_key, card_number, topup_amount, topup_method, status) "
                + "VALUES ($1, $2, $3, 'BANK_TRANSFER', 'SUCCESS') "
                + "ON CONFLICT DO NOTHING";

        return pg.preparedQuery(sql).execute(Tuple.of("seed-topup-1", "4111111111111111", 250_000))
                .chain(r -> pg.preparedQuery(sql).execute(Tuple.of("seed-topup-2", "4111111111111112", 100_000)))
                .replaceWithVoid();
    }
}
