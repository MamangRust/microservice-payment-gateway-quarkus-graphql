package com.sanedge.statswriter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.common.clickhouse.ClickHouseClient;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Buffers rows per ClickHouse table and flushes them as {@code JSONEachRow}
 * batches. Mirrors the Go reference batching (batch size 1000 / flush every
 * 5s): rows are appended into a per-table INSERT statement and sent on flush,
 * keeping ClickHouse write amplification low.
 */
@ApplicationScoped
public class ClickHouseBatchWriter {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseBatchWriter.class);

    @Inject
    ClickHouseClient clickHouse;

    @Inject
    StatsWriterMetrics metrics;

    @ConfigProperty(name = "stats.batch-size", defaultValue = "1000")
    int batchSize;

    private final Map<String, List<String>> batches = new LinkedHashMap<>();

    /** Appends one row (JSON object) to the given table's pending batch. */
    public synchronized Uni<Void> append(String table, JsonObject row) {
        List<String> rows = batches.computeIfAbsent(table, k -> new ArrayList<>());
        rows.add(row.encode());
        if (rows.size() >= batchSize) {
            return flushTable(table);
        }
        return Uni.createFrom().voidItem();
    }

    /** Flushes all pending tables (called by the scheduler and on shutdown). */
    public synchronized Uni<Void> flushAll() {
        Uni<Void> chain = Uni.createFrom().voidItem();
        for (String table : new ArrayList<>(batches.keySet())) {
            chain = chain.chain(() -> flushTable(table));
        }
        return chain;
    }

    private Uni<Void> flushTable(String table) {
        List<String> rows = batches.remove(table);
        if (rows == null || rows.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        StringBuilder body = new StringBuilder();
        for (String row : rows) {
            body.append(row).append('\n');
        }
        String insert = "INSERT INTO " + table + " FORMAT JSONEachRow";
        return clickHouse.execute(insert, body.toString())
                .invoke(() -> log.debug("Flushed {} rows to ClickHouse {}", rows.size(), table))
                .onFailure().invoke(err -> {
                    log.error("ClickHouse flush failed for {} ({} rows)", table, rows.size(), err);
                    metrics.recordFlushError();
                    // Re-buffer on failure so the next flush retries (at-least-once).
                    synchronized (this) {
                        List<String> existing = batches.computeIfAbsent(table, k -> new ArrayList<>());
                        rows.forEach(existing::add);
                    }
                })
                .invoke(() -> metrics.setBatchPending(pendingCount()))
                .replaceWithVoid();
    }

    public synchronized int pendingCount() {
        return batches.values().stream().mapToInt(List::size).sum();
    }
}
