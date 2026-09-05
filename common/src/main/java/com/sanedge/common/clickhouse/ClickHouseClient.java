package com.sanedge.common.clickhouse;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Minimal reactive ClickHouse client over the HTTP interface (port 8123).
 *
 * <p>Used by the stats pipeline: {@code stats-writer} inserts rows in
 * {@code JSONEachRow} batches, {@code stats-reader} executes SELECTs with
 * {@code FORMAT JSON}. All calls are non-blocking Vert.x HTTP requests, so
 * they are safe on event-loop threads (matching the reactive stack of the
 * rest of the project).
 */
@ApplicationScoped
public class ClickHouseClient {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseClient.class);

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "clickhouse.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "clickhouse.http-port", defaultValue = "8123")
    int httpPort;

    @ConfigProperty(name = "clickhouse.database", defaultValue = "payment_stats")
    String database;

    @ConfigProperty(name = "clickhouse.username", defaultValue = "default")
    String username;

    @ConfigProperty(name = "clickhouse.password", defaultValue = "none")
    String password;

    private HttpClient client;

    @PostConstruct
    void init() {
        client = vertx.createHttpClient();
        log.info("ClickHouseClient initialized. http://{}:{}/{}", host, httpPort, database);
    }

    @PreDestroy
    void destroy() {
        if (client != null) {
            client.close();
        }
    }

    /**
     * Executes a query and returns the raw response body (use {@code FORMAT JSON}
     * in the query to get parseable output).
     */
    public Uni<String> query(String sql) {
        String path = "/?database=" + urlEncode(database)
                + "&query=" + urlEncode(sql)
                + auth();
        return request(HttpMethod.GET, path, null);
    }

    /**
     * Executes a query with a request body (INSERT ... FORMAT JSONEachRow).
     * Pass {@code null} to send an empty POST body (ClickHouse rejects POSTs
     * without a Content-Length header, so an empty buffer is sent instead).
     */
    public Uni<String> execute(String sql, String body) {
        String path = "/?database=" + urlEncode(database)
                + "&query=" + urlEncode(sql)
                + auth();
        return request(HttpMethod.POST, path, body == null ? "" : body);
    }

    /**
     * Executes a query without binding the {@code database} URL parameter.
     * Needed for statements like {@code CREATE DATABASE}, which ClickHouse
     * refuses when the target database does not exist yet (the database param
     * is validated before the query runs).
     */
    public Uni<String> executeNoDatabase(String sql, String body) {
        String path = "/?query=" + urlEncode(sql) + auth();
        return request(HttpMethod.POST, path, body == null ? "" : body);
    }

    private Uni<String> request(HttpMethod method, String path, String body) {
        Uni<HttpClientRequest> req = client.request(method, httpPort, host, path);
        return req.flatMap(r -> {
            // Force header map initialization: Quarkus OTel's Vert.x tracer reads
            // HttpRequestHead.headers on send; if it was never touched it is null
            // and the tracer NPEs ("Cannot invoke MultiMap.entries()...").
            r.headers();
            if (body != null) {
                return r.send(Buffer.buffer(body));
            }
            return r.send();
        }).flatMap(this::readBody);
    }

    private Uni<String> readBody(HttpClientResponse resp) {
        return resp.body().map(b -> {
            String text = b.toString();
            if (resp.statusCode() >= 400) {
                throw new IllegalStateException("ClickHouse HTTP " + resp.statusCode() + ": " + truncate(text));
            }
            return text;
        });
    }

    private String auth() {
        StringBuilder sb = new StringBuilder();
        if (username != null && !username.isBlank()) {
            sb.append("&user=").append(urlEncode(username));
        }
        if (password != null && !password.isBlank() && !"none".equals(password)) {
            sb.append("&password=").append(urlEncode(password));
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static String truncate(String s) {
        return s != null && s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
