package com.sanedge.merchant.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Merchant seeder: demo merchants linked to the identity users.
 *
 * <p>Idempotent: {@code ON CONFLICT (api_key)} DO NOTHING. Requires the
 * identity seeder to have run first (user_id is resolved via subquery).
 * Domain {@code merchant} (order 20).</p>
 */
public class MerchantSeeder implements Seeder {

    @Override
    public String domain() {
        return "merchant";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_merchant.merchants (name, api_key, user_id, status) "
                + "VALUES ($1, $2, (SELECT id FROM payment_identity.users WHERE username = $3), 'ACTIVE') "
                + "ON CONFLICT (api_key) DO NOTHING";

        return pg.preparedQuery(sql).execute(Tuple.of("Admin Merchant", "seed-admin-merchant-key", "admin"))
                .chain(r -> pg.preparedQuery(sql).execute(Tuple.of("User Merchant", "seed-user-merchant-key", "user")))
                .replaceWithVoid();
    }
}
