package com.sanedge.card.seed;

import com.sanedge.common.seed.SeedContext;
import com.sanedge.common.seed.Seeder;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.Tuple;

/**
 * Card seeder: demo cards linked to the identity users.
 *
 * <p>Idempotent: {@code ON CONFLICT (card_number)} DO NOTHING. Requires the
 * identity seeder to have run first. Domain {@code card} (order 20).</p>
 */
public class CardSeeder implements Seeder {

    @Override
    public String domain() {
        return "card";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Uni<Void> seed(SeedContext ctx) {
        var pg = ctx.pool();

        String sql = "INSERT INTO payment_card.cards "
                + "(user_id, card_number, card_type, expire_date, cvv, card_provider, status, credit_limit, points) "
                + "VALUES ((SELECT id FROM payment_identity.users WHERE username = $1), $2, 'VISA', '2030-12-31', '123', 'VISA', 'ACTIVE', 5000000, 0) "
                + "ON CONFLICT (card_number) DO NOTHING";

        return pg.preparedQuery(sql).execute(Tuple.of("admin", "4111111111111111"))
                .chain(r -> pg.preparedQuery(sql).execute(Tuple.of("user", "4111111111111112")))
                .replaceWithVoid();
    }
}
