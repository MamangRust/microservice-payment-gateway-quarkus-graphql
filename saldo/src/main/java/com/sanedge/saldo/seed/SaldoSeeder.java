package com.sanedge.saldo.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Saldo seeder: opening balances for the seeded cards.
 *
 * <p>Idempotent via the partial unique index on active card numbers
 * ({@code idx_saldos_active_card_number}). Requires the card seeder to have
 * run first. Domain {@code saldo} (order 30).</p>
 */
public class SaldoSeeder implements Seeder {

    @Override
    public String domain() {
        return "saldo";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_finance.saldos (card_number, total_balance, withdraw_amount) "
                + "VALUES ($1, $2, 0) "
                + "ON CONFLICT (card_number) WHERE deleted_at IS NULL DO NOTHING";

        return pg.preparedQuery(sql).execute(Tuple.of("4111111111111111", 1_000_000))
                .chain(r -> pg.preparedQuery(sql).execute(Tuple.of("4111111111111112", 500_000)))
                .replaceWithVoid();
    }
}
