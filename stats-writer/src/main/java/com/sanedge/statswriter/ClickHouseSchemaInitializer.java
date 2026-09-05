package com.sanedge.statswriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.clickhouse.ClickHouseClient;

import io.quarkus.runtime.Startup;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Ensures the ClickHouse stats tables exist on startup. Port of the Go
 * reference {@code pkg/clickhouse/schema.sql} — MergeTree, partitioned by
 * month, ordered for the OLAP queries stats-reader runs.
 */
@Startup
@ApplicationScoped
public class ClickHouseSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseSchemaInitializer.class);

    @Inject
    ClickHouseClient clickHouse;

    @PostConstruct
    void init() {
        createSchema()
                .subscribe().with(
                        v -> log.info("ClickHouse schema ready"),
                        err -> log.error("ClickHouse schema init failed (retrying at next flush): {}", err, err));
    }

    public Uni<Void> createSchema() {
        Uni<Void> chain = Uni.createFrom().voidItem();
        chain = chain.chain(() -> clickHouse.executeNoDatabase("CREATE DATABASE IF NOT EXISTS payment_stats", null).replaceWithVoid());
        for (String ddl : schemaStatements()) {
            chain = chain.chain(() -> clickHouse.execute(ddl, null).replaceWithVoid());
        }
        return chain;
    }

    static String[] schemaStatements() {
        // Ids are String in the outbox payloads (e.g. "txn-0001"), so the
        // UInt64 columns from the Go reference become String here to match the
        // actual event shape.
        return new String[] {
                "CREATE TABLE IF NOT EXISTS transaction_events ("
                        + "transaction_id String, transaction_no String, card_number String, card_type String,"
                        + "card_provider String, amount Int64, payment_method String, merchant_id String,"
                        + "merchant_name String, status String, apikey String, created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (merchant_id, created_at)",
                "CREATE TABLE IF NOT EXISTS topup_events ("
                        + "topup_id String, topup_no String, card_number String, card_type String,"
                        + "card_provider String, amount Int64, payment_method String, status String,"
                        + "created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (card_number, created_at)",
                "CREATE TABLE IF NOT EXISTS transfer_events ("
                        + "transfer_id String, transfer_no String, source_card String, destination_card String,"
                        + "amount Int64, status String, created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (source_card, created_at)",
                "CREATE TABLE IF NOT EXISTS withdraw_events ("
                        + "withdraw_id String, withdraw_no String, card_number String, card_type String,"
                        + "amount Int64, status String, created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (card_number, created_at)",
                "CREATE TABLE IF NOT EXISTS saldo_events ("
                        + "card_number String, total_balance Int64, created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (card_number, created_at)",
                "CREATE TABLE IF NOT EXISTS card_events ("
                        + "card_id String, user_id String, card_number String, card_type String,"
                        + "card_provider String, status String, created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (user_id, created_at)",
                "CREATE TABLE IF NOT EXISTS merchant_events ("
                        + "merchant_id String, user_id String, name String, email String, status String,"
                        + "created_at DateTime DEFAULT now()"
                        + ") ENGINE = MergeTree() PARTITION BY toYYYYMM(created_at) ORDER BY (user_id, created_at)"
        };
    }
}
