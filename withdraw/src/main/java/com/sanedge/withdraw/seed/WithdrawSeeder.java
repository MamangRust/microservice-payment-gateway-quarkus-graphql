package com.sanedge.withdraw.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Withdraw seeder: demo withdrawals for the seeded cards.
 *
 * <p>Idempotent via the partial unique index on {@code idempotency_key}
 * ({@code idx_withdraws_idempotency_key}). Requires the card seeder to have
 * run first. Domain {@code withdraw} (order 40).</p>
 */
public class WithdrawSeeder implements Seeder {

    @Override
    public String domain() {
        return "withdraw";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_finance.withdraws "
                + "(idempotency_key, card_number, withdraw_amount, status) "
                + "VALUES ($1, $2, $3, 'SUCCESS') "
                + "ON CONFLICT DO NOTHING";

        return pg.preparedQuery(sql).execute(Tuple.of("seed-withdraw-1", "4111111111111111", 100_000))
                .replaceWithVoid();
    }
}
