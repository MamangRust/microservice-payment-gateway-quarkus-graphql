package com.sanedge.transaction.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Transaction seeder: demo purchase transactions for the seeded cards,
 * linked to the seeded merchants.
 *
 * <p>Idempotent via the partial unique index on {@code idempotency_key}
 * ({@code idx_transactions_idempotency_key}). Requires the card and merchant
 * seeders to have run first. Domain {@code transaction} (order 40).</p>
 */
public class TransactionSeeder implements Seeder {

    @Override
    public String domain() {
        return "transaction";
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_finance.transactions "
                + "(idempotency_key, card_number, amount, payment_method, merchant_id, status) "
                + "VALUES ($1, $2, $3, $4, "
                + "(SELECT merchant_id FROM payment_merchant.merchants WHERE api_key = $5), 'SUCCESS') "
                + "ON CONFLICT DO NOTHING";

        return pg.preparedQuery(sql)
                .execute(Tuple.of("seed-txn-1", "4111111111111111", 75_000, "QRIS", "seed-admin-merchant-key"))
                .chain(r -> pg.preparedQuery(sql)
                        .execute(Tuple.of("seed-txn-2", "4111111111111112", 45_000, "CARD", "seed-user-merchant-key")))
                .replaceWithVoid();
    }
}
