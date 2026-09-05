package com.sanedge.transfer.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Transfer seeder: demo fund transfers between the seeded cards.
 *
 * <p>Idempotent via the partial unique index on {@code idempotency_key}
 * ({@code idx_transfers_idempotency_key}). Requires the card seeder to have
 * run first. Domain {@code transfer} (order 40).</p>
 */
public class TransferSeeder implements Seeder {

    @Override
    public String domain() {
        return "transfer";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_finance.transfers "
                + "(idempotency_key, transfer_from, transfer_to, transfer_amount, status) "
                + "VALUES ($1, $2, $3, $4, 'SUCCESS') "
                + "ON CONFLICT DO NOTHING";

        return pg.preparedQuery(sql)
                .execute(Tuple.of("seed-transfer-1", "4111111111111111", "4111111111111112", 50_000))
                .replaceWithVoid();
    }
}
