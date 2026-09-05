package com.sanedge.statsreader.handler;

import io.vertx.core.json.JsonObject;

/**
 * Static helpers for reading values from ClickHouse JSON result rows.
 *
 * <p>ClickHouse {@code FORMAT JSON} serialises every value as a JSON string
 * (e.g. {@code "total_amount": "650000"}), so the numeric helpers must be
 * tolerant of both numbers and numeric strings.
 */
public final class StatsRow {

    private StatsRow() {
    }

    public static int intOf(JsonObject row, String key) {
        Object v = row.getValue(key);
        if (v == null) {
            return 0;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        return (int) Math.round(Double.parseDouble(String.valueOf(v).trim()));
    }

    public static long longOf(JsonObject row, String key) {
        Object v = row.getValue(key);
        if (v == null) {
            return 0L;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return Math.round(Double.parseDouble(String.valueOf(v).trim()));
    }

    public static String strOf(JsonObject row, String key) {
        Object v = row.getValue(key);
        return v == null ? "" : String.valueOf(v);
    }
}
