package com.sanedge.statsreader.repository;

import java.util.List;

import com.sanedge.common.clickhouse.ClickHouseClient;
import com.sanedge.statsreader.cache.StatsCache;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Generic ClickHouse reader — port of the Go {@code stats-reader}
 * repository. All queries run over the HTTP interface with {@code FORMAT JSON}
 * and are parsed into {@link JsonArray}/{@link JsonObject} rows.
 *
 * <p>Query results are cached (cache-aside) under the {@code apigw:stats:}
 * namespace via {@link StatsCache}; cache misses or Redis outages fall back to
 * ClickHouse transparently.
 */
@ApplicationScoped
public class ClickHouseStatsRepository {

    @Inject
    ClickHouseClient clickHouse;

    @Inject
    StatsCache statsCache;

    // ---------- Amounts (monthly / yearly) ----------

    public Uni<JsonArray> monthlyAmounts(String table, String filterField, Object filterValue, int year) {
        String where = (table.equals("saldo_events") ? "toYear(created_at) = " + year
                : "toYear(created_at) = " + year + " AND status = 'success'");
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String amountCol = table.equals("saldo_events") ? "total_balance" : "amount";
        String sql = "SELECT toString(toYear(created_at)) AS year, formatDateTime(created_at, '%b') AS month, "
                + "sum(" + amountCol + ") AS total_amount FROM " + table + " WHERE " + where
                + " GROUP BY year, month, toMonth(created_at) ORDER BY year, toMonth(created_at) FORMAT JSON";
        return query(sql);
    }

    public Uni<JsonArray> yearlyAmounts(String table, String filterField, Object filterValue, int startYear, int endYear) {
        String where = (table.equals("saldo_events")
                ? "toYear(created_at) >= " + startYear + " AND toYear(created_at) <= " + endYear
                : "toYear(created_at) >= " + startYear + " AND toYear(created_at) <= " + endYear
                        + " AND status = 'success'");
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String amountCol = table.equals("saldo_events") ? "total_balance" : "amount";
        String sql = "SELECT toString(toYear(created_at)) AS year, sum(" + amountCol + ") AS total_amount FROM "
                + table + " WHERE " + where + " GROUP BY year ORDER BY year FORMAT JSON";
        return query(sql);
    }

    // ---------- Payment method stats (monthly / yearly) ----------

    public Uni<JsonArray> monthlyMethodStats(String table, String filterField, Object filterValue, int year) {
        String where = "toYear(created_at) = " + year + " AND status = 'success'";
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String sql = "SELECT formatDateTime(created_at, '%b') AS month, payment_method AS method, "
                + "count() AS total_transactions, sum(amount) AS total_amount FROM " + table + " WHERE " + where
                + " GROUP BY month, method, toMonth(created_at) ORDER BY toMonth(created_at), method FORMAT JSON";
        return query(sql);
    }

    public Uni<JsonArray> yearlyMethodStats(String table, String filterField, Object filterValue, int startYear,
            int endYear) {
        String where = "toYear(created_at) >= " + startYear + " AND toYear(created_at) <= " + endYear
                + " AND status = 'success'";
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String sql = "SELECT toString(toYear(created_at)) AS year, payment_method AS method, "
                + "count() AS total_transactions, sum(amount) AS total_amount FROM " + table + " WHERE " + where
                + " GROUP BY year, method ORDER BY year, method FORMAT JSON";
        return query(sql);
    }

    // ---------- Status stats (monthly / yearly, success|failed) ----------

    public Uni<JsonArray> monthlyStatusStats(String table, String filterField, Object filterValue, int year,
            String status) {
        String where = "toYear(created_at) = " + year;
        if (status != null && !status.isBlank()) {
            where += " AND status = '" + status + "'";
        }
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String sql = "SELECT toString(toYear(created_at)) AS year, formatDateTime(created_at, '%b') AS month, "
                + "count() AS total_transactions, sum(amount) AS total_amount FROM " + table + " WHERE " + where
                + " GROUP BY year, month, toMonth(created_at) ORDER BY year, toMonth(created_at) FORMAT JSON";
        return query(sql);
    }

    public Uni<JsonArray> yearlyStatusStats(String table, String filterField, Object filterValue, int currentYear,
            String status) {
        String where = "(toYear(created_at) = " + currentYear + " OR toYear(created_at) = " + (currentYear - 1) + ")";
        if (status != null && !status.isBlank()) {
            where += " AND status = '" + status + "'";
        }
        if (filterField != null && !filterField.isBlank()) {
            where += " AND " + filterField + " = '" + filterValue + "'";
        }
        String sql = "SELECT toString(toYear(created_at)) AS year, count() AS total_transactions, "
                + "sum(amount) AS total_amount FROM " + table + " WHERE " + where
                + " GROUP BY year ORDER BY year DESC FORMAT JSON";
        return query(sql);
    }

    // ---------- Saldo (latest balance per card, monthly / yearly) ----------

    public Uni<JsonArray> monthlyTotalSaldo(int year) {
        String sql = "SELECT toString(toYear(created_at)) AS year, formatDateTime(created_at, '%b') AS month, "
                + "sum(total_balance) AS total_amount FROM ("
                + "SELECT card_number, total_balance, created_at, "
                + "ROW_NUMBER() OVER (PARTITION BY card_number, formatDateTime(created_at, '%Y-%m') "
                + "ORDER BY created_at DESC) AS rn FROM saldo_events WHERE toYear(created_at) = " + year + ") "
                + "WHERE rn = 1 GROUP BY year, month, toMonth(created_at) ORDER BY year, toMonth(created_at) FORMAT JSON";
        return query(sql);
    }

    public Uni<JsonArray> yearlyTotalSaldo(int startYear, int endYear) {
        String sql = "SELECT toString(toYear(created_at)) AS year, sum(total_balance) AS total_amount FROM ("
                + "SELECT card_number, total_balance, created_at, "
                + "ROW_NUMBER() OVER (PARTITION BY card_number, toString(toYear(created_at)) "
                + "ORDER BY created_at DESC) AS rn FROM saldo_events "
                + "WHERE toYear(created_at) >= " + startYear + " AND toYear(created_at) <= " + endYear + ") "
                + "WHERE rn = 1 GROUP BY year ORDER BY year FORMAT JSON";
        return query(sql);
    }

    // ---------- Merchant transactions (dashboard list) ----------

    public Uni<JsonArray> merchantTransactions(Integer merchantId, String apiKey) {
        String where = "status = 'success'";
        if (merchantId != null && merchantId != 0) {
            where += " AND merchant_id = " + merchantId;
        }
        if (apiKey != null && !apiKey.isBlank()) {
            where += " AND apikey = '" + apiKey + "'";
        }
        String sql = "SELECT transaction_id, amount, payment_method AS method, toString(created_at) AS created_at "
                + "FROM transaction_events WHERE " + where + " ORDER BY created_at DESC LIMIT 100 FORMAT JSON";
        return query(sql);
    }

    // ---------- Transfer by card (sender/receiver) ----------

    public Uni<JsonArray> monthlyTransferByCard(String cardColumn, String cardNumber, int year) {
        String sql = "SELECT toString(toYear(created_at)) AS year, formatDateTime(created_at, '%b') AS month, "
                + "sum(amount) AS total_amount FROM transfer_events "
                + "WHERE toYear(created_at) = " + year + " AND status = 'success' AND " + cardColumn + " = '"
                + cardNumber + "'"
                + " GROUP BY year, month, toMonth(created_at) ORDER BY year, toMonth(created_at) FORMAT JSON";
        return query(sql);
    }

    public Uni<JsonArray> yearlyTransferByCard(String cardColumn, String cardNumber, int year) {
        String sql = "SELECT toString(toYear(created_at)) AS year, sum(amount) AS total_amount FROM transfer_events "
                + "WHERE toYear(created_at) = " + year + " AND status = 'success' AND " + cardColumn + " = '"
                + cardNumber + "'"
                + " GROUP BY year ORDER BY year FORMAT JSON";
        return query(sql);
    }

    // ---------- Card dashboard (aggregates across event tables) ----------

    public Uni<JsonObject> dashboard(String cardNumber) {
        String cardWhere = (cardNumber != null && !cardNumber.isBlank())
                ? " AND card_number = '" + cardNumber + "'"
                : "";
        String balanceWhere = (cardNumber != null && !cardNumber.isBlank())
                ? " AND card_number = '" + cardNumber + "'"
                : "";
        String transferSenderWhere = (cardNumber != null && !cardNumber.isBlank())
                ? " AND source_card = '" + cardNumber + "'"
                : "";
        String transferReceiverWhere = (cardNumber != null && !cardNumber.isBlank())
                ? " AND destination_card = '" + cardNumber + "'"
                : "";
        String transferAnyWhere = (cardNumber != null && !cardNumber.isBlank())
                ? " AND (source_card = '" + cardNumber + "' OR destination_card = '" + cardNumber + "')"
                : "";
        String sql = "SELECT "
                + "(SELECT sum(total_balance) FROM (SELECT card_number, total_balance, "
                + "ROW_NUMBER() OVER (PARTITION BY card_number ORDER BY created_at DESC) AS rn "
                + "FROM saldo_events WHERE 1=1" + balanceWhere + ") WHERE rn = 1) AS total_balance, "
                + "(SELECT sum(amount) FROM topup_events WHERE status = 'success'" + cardWhere + ") AS total_topup, "
                + "(SELECT sum(amount) FROM withdraw_events WHERE status = 'success'" + cardWhere + ") AS total_withdraw, "
                + "(SELECT sum(amount) FROM transaction_events WHERE status = 'success'" + cardWhere + ") AS total_transaction, "
                + "(SELECT sum(amount) FROM transfer_events WHERE status = 'success'" + transferAnyWhere + ") AS total_transfer, "
                + "(SELECT sum(amount) FROM transfer_events WHERE status = 'success'" + transferSenderWhere + ") AS total_transfer_send, "
                + "(SELECT sum(amount) FROM transfer_events WHERE status = 'success'" + transferReceiverWhere + ") AS total_transfer_receiver "
                + "FORMAT JSON";
        String key = "dashboard:" + StatsCache.hash(sql);
        return statsCache.cacheObject(key, () ->
                clickHouse.query(sql).map(body -> {
                    JsonObject json = new JsonObject(body);
                    JsonArray data = json.getJsonArray("data");
                    if (data != null && data.size() > 0) {
                        Object first = data.getValue(0);
                        if (first instanceof JsonObject obj) {
                            return obj;
                        }
                    }
                    return new JsonObject();
                }));
    }

    // ---------- Raw query helper ----------

    public Uni<JsonArray> query(String sql) {
        String key = "query:" + StatsCache.hash(sql);
        return statsCache.cacheArray(key, () -> clickHouse.query(sql).map(body -> {
            JsonObject json = new JsonObject(body);
            JsonArray data = json.getJsonArray("data");
            return data != null ? data : new JsonArray();
        }));
    }

    public Uni<JsonArray> queryRows(String sql, List<Object> args) {
        return query(sql);
    }
}
