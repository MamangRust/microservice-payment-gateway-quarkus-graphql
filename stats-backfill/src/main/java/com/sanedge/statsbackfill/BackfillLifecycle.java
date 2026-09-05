package com.sanedge.statsbackfill;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.sqlclient.Pool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * One-shot stats backfill (Quarkus equivalent of the F4 backfill step).
 *
 * <p>Reads historical rows from the OLTP tables and enqueues one stats event
 * per row into the domain's {@code outbox} table (status PENDING) with a
 * deterministic {@code event_id} ({@code backfill:&lt;domain&gt;:&lt;id&gt;}).
 * The existing F3 {@code OutboxPublisher} then relays them to Kafka
 * ({@code stats.payment.*.event}) → stats-writer → ClickHouse. Writing through
 * the outbox gives idempotency for free: re-running the backfill hits the
 * unique {@code event_id} constraint and inserts nothing.
 *
 * <p>Usage: {@code BACKFILL_DOMAINS=transaction,merchant BACKFILL_FROM=2024-01-01T00:00:00Z
 * java -jar stats-backfill/target/quarkus-app/quarkus-run.jar}
 */
@ApplicationScoped
public class BackfillLifecycle {

    private static final Logger LOGGER = Logger.getLogger(BackfillLifecycle.class);

    /** Domain → (OLTP table, outbox schema.table). */
    private static final List<DomainSpec> DOMAINS = List.of(
            new DomainSpec("transaction", "payment_finance.transactions", "payment_finance.outbox",
                    "stats.payment.transaction.event"),
            new DomainSpec("topup", "payment_finance.topups", "payment_finance.outbox",
                    "stats.payment.topup.event"),
            new DomainSpec("transfer", "payment_finance.transfers", "payment_finance.outbox",
                    "stats.payment.transfer.event"),
            new DomainSpec("withdraw", "payment_finance.withdraws", "payment_finance.outbox",
                    "stats.payment.withdraw.event"),
            new DomainSpec("saldo", "payment_finance.saldos", "payment_finance.outbox",
                    "stats.payment.saldo.event"),
            new DomainSpec("merchant", "payment_merchant.merchants", "payment_merchant.outbox",
                    "stats.payment.merchant.event"),
            new DomainSpec("card", "payment_card.cards", "payment_card.outbox",
                    "stats.payment.card.event"));

    @Inject
    Pool pool;

    @ConfigProperty(name = "backfill.domains", defaultValue = "all")
    String domainsCfg;

    @ConfigProperty(name = "backfill.from", defaultValue = "none")
    String fromCfg;

    void onStart(@Observes StartupEvent ev) {
        List<String> filter = Arrays.stream(domainsCfg.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank() && !"all".equalsIgnoreCase(s))
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();

        List<DomainSpec> targets = new ArrayList<>();
        for (DomainSpec spec : DOMAINS) {
            if (filter.isEmpty() || filter.contains(spec.domain)) {
                targets.add(spec);
            }
        }

        LOGGER.infof("Stats backfill started. Domains: %s, from: %s",
                targets.stream().map(DomainSpec::domain).toList(),
                "none".equalsIgnoreCase(fromCfg) ? "(all)" : fromCfg);

        runBackfill(targets)
                .subscribe().with(
                        v -> {
                            LOGGER.info("Stats backfill completed successfully.");
                            Quarkus.asyncExit(0);
                        },
                        err -> {
                            LOGGER.error("Stats backfill failed: " + err.getMessage(), err);
                            Quarkus.asyncExit(1);
                        });
    }

    private Uni<Void> runBackfill(List<DomainSpec> targets) {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (DomainSpec spec : targets) {
            chain = chain.chain(() -> backfillDomain(spec));
        }
        return chain;
    }

    private Uni<Void> backfillDomain(DomainSpec spec) {
        return pool.query("SELECT * FROM " + spec.oltpTable + whereClause() + " ORDER BY created_at")
                .execute()
                .flatMap(rows -> enqueue(rows, spec));
    }

    private String whereClause() {
        if (fromCfg.isBlank() || "none".equalsIgnoreCase(fromCfg)) {
            return "";
        }
        return " WHERE created_at >= '" + fromCfg + "'";
    }

    /** Inserts one outbox row per OLTP row with a deterministic event_id. */
    private Uni<Void> enqueue(RowSet<Row> rows, DomainSpec spec) {
        List<Uni<Void>> inserts = new ArrayList<>();
        int[] skipped = { 0 };
        for (Row row : rows) {
            // OLTP PK is <domain>_id (transaction_id, topup_id, ...); the
            // deterministic event_id makes re-runs idempotent via the unique
            // outbox constraint.
            Object pk = row.getValue(spec.domain + "_id");
            String eventId = "backfill:" + spec.domain + ":" + (pk == null ? "null" : pk);
            String eventKey = pk == null ? eventId : String.valueOf(pk);
            JsonObject payload = buildPayload(spec, row);
            if (payload == null) {
                skipped[0]++;
                continue;
            }
            inserts.add(enqueueEvent(spec, eventId, eventKey, payload));
        }
        if (inserts.isEmpty()) {
            LOGGER.infof("  %s: 0 rows (skipped %d)", spec.domain, skipped[0]);
            return Uni.createFrom().voidItem();
        }
        return Uni.combine().all().unis(inserts).discardItems()
                .invoke(() -> LOGGER.infof("  %s: enqueued %d event(s)", spec.domain, inserts.size()));
    }

    private Uni<Void> enqueueEvent(DomainSpec spec, String eventId, String eventKey, JsonObject payload) {
        String sql = "INSERT INTO " + spec.outboxTable
                + " (domain, event_id, topic, event_key, payload, status, attempts, next_attempt_at, created_at, updated_at) "
                + "VALUES ($1, $2, $3, $4, $5, 'PENDING', 0, now(), now(), now()) "
                + "ON CONFLICT (event_id) DO NOTHING";
        return pool.preparedQuery(sql)
                .execute(io.vertx.mutiny.sqlclient.Tuple.of(
                        spec.domain, eventId, spec.topic, eventKey,
                        payload.encode()))
                .replaceWithVoid();
    }

    /**
     * Maps OLTP row → the flattened EventEnvelope payload written by the F3
     * command services (field names = ClickHouse columns).
     */
    private JsonObject buildPayload(DomainSpec spec, Row row) {
        JsonObject p = new JsonObject();
        String occurred = row.getValue("created_at") != null
                ? String.valueOf(row.getValue("created_at"))
                : Instant.now().toString();
        switch (spec.domain) {
            case "transaction" -> {
                p.put("transaction_id", String.valueOf(row.getValue("transaction_id")));
                p.put("transaction_no", String.valueOf(row.getValue("transaction_no")));
                p.put("card_number", str(row, "card_number"));
                p.put("amount", row.getInteger("amount"));
                p.put("payment_method", str(row, "payment_method"));
                p.put("merchant_id", String.valueOf(row.getValue("merchant_id")));
                p.put("status", lower(row, "status"));
            }
            case "topup" -> {
                p.put("topup_id", String.valueOf(row.getValue("topup_id")));
                p.put("topup_no", String.valueOf(row.getValue("topup_no")));
                p.put("card_number", str(row, "card_number"));
                p.put("amount", row.getInteger("topup_amount"));
                p.put("payment_method", str(row, "topup_method"));
                p.put("status", lower(row, "status"));
            }
            case "transfer" -> {
                p.put("transfer_id", String.valueOf(row.getValue("transfer_id")));
                p.put("transfer_no", String.valueOf(row.getValue("transfer_no")));
                p.put("source_card", str(row, "transfer_from"));
                p.put("destination_card", str(row, "transfer_to"));
                p.put("amount", row.getInteger("transfer_amount"));
                p.put("status", lower(row, "status"));
            }
            case "withdraw" -> {
                p.put("withdraw_id", String.valueOf(row.getValue("withdraw_id")));
                p.put("withdraw_no", String.valueOf(row.getValue("withdraw_no")));
                p.put("card_number", str(row, "card_number"));
                p.put("amount", row.getInteger("withdraw_amount"));
                p.put("status", lower(row, "status"));
            }
            case "saldo" -> {
                p.put("card_number", str(row, "card_number"));
                p.put("total_balance", row.getInteger("total_balance"));
            }
            case "merchant" -> {
                p.put("merchant_id", String.valueOf(row.getValue("merchant_id")));
                p.put("user_id", String.valueOf(row.getValue("user_id")));
                p.put("name", str(row, "name"));
                p.put("status", lower(row, "status"));
            }
            case "card" -> {
                p.put("card_id", String.valueOf(row.getValue("card_id")));
                p.put("user_id", String.valueOf(row.getValue("user_id")));
                p.put("card_number", str(row, "card_number"));
                p.put("card_type", str(row, "card_type"));
                p.put("card_provider", str(row, "card_provider"));
                p.put("status", lower(row, "status"));
            }
            default -> {
                return null;
            }
        }
        p.put("created_at", occurred);
        return com.sanedge.common.event.EventEnvelope.withDefaults(p, spec.domain + ".created");
    }

    private static String str(Row row, String col) {
        Object v = row.getValue(col);
        return v == null ? "" : String.valueOf(v);
    }

    private static String lower(Row row, String col) {
        Object v = row.getValue(col);
        return v == null ? "" : String.valueOf(v).toLowerCase(Locale.ROOT);
    }

    private record DomainSpec(String domain, String oltpTable, String outboxTable, String topic) {
    }
}
