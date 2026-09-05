package com.sanedge.statswriter;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import io.vertx.mutiny.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.runtime.Startup;

/**
 * Consumes {@code stats.payment.*.event} topics (produced by the F3 outbox
 * relay), unwraps the shared {@code EventEnvelope}, dedups by {@code event_id}
 * and routes each event to its ClickHouse table via {@link ClickHouseBatchWriter}.
 *
 * <p>Event envelope shape (flattened): {@code event_id, schema_version,
 * event_type, occurred_at} + business fields at the root — see
 * {@code com.sanedge.common.event.EventEnvelope}.
 */
@Startup
@ApplicationScoped
public class StatsKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(StatsKafkaConsumer.class);

    private static final String[] DOMAINS = { "transaction", "topup", "transfer", "withdraw", "saldo", "merchant", "card" };

    @Inject
    Vertx vertx;

    @Inject
    ClickHouseBatchWriter batchWriter;

    @Inject
    EventDedup dedup;

    @Inject
    StatsWriterMetrics metrics;

    @ConfigProperty(name = "kafka.bootstrap.servers", defaultValue = "localhost:9092")
    String bootstrapServers;

    @ConfigProperty(name = "kafka.group.id", defaultValue = "stats-writer-group")
    String groupId;

    @ConfigProperty(name = "stats.topics", defaultValue = "")
    List<String> topics;

    private KafkaConsumer<String, String> consumer;

    @PostConstruct
    void init() {
        Map<String, String> config = new HashMap<>();
        config.put("bootstrap.servers", bootstrapServers);
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", groupId);
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit", "true");

        consumer = KafkaConsumer.create(vertx.getDelegate(), config);
        consumer.handler(this::onRecord);
        consumer.subscribe(new java.util.HashSet<>(topics))
                .onSuccess(v -> log.info("StatsWriter subscribed to {} topics: {}", topics.size(), topics))
                .onFailure(err -> log.error("Failed to subscribe stats topics", err));
    }

    private void onRecord(KafkaConsumerRecord<String, String> record) {
        String topic = record.topic();
        String value = record.value();
        if (value == null || value.isBlank()) {
            return;
        }
        try {
            JsonObject event = new JsonObject(value);
            String eventId = event.getString("event_id");
            if (dedup.isDuplicate(eventId)) {
                return;
            }
            String table = tableForTopic(topic);
            if (table == null) {
                log.warn("No ClickHouse table for topic {}", topic);
                return;
            }
            JsonObject row = normalizeRow(event);
            metrics.recordEventConsumed();
            batchWriter.append(table, row)
                    .subscribe().with(v -> { }, err -> log.error("Failed to enqueue {} row", table, err));
        } catch (Exception e) {
            log.error("Failed to process record from {}: {}", topic, e.getMessage());
        }
    }

    private String tableForTopic(String topic) {
        for (String domain : DOMAINS) {
            if (topic.contains(domain)) {
                return domain + "_events";
            }
        }
        return null;
    }

    /** Drops envelope meta fields, keeps business fields + occurred_at → created_at. */
    private JsonObject normalizeRow(JsonObject event) {
        JsonObject row = event.copy();
        row.remove("event_id");
        row.remove("schema_version");
        row.remove("event_type");
        String occurredAt = row.getString("occurred_at");
        row.remove("occurred_at");
        if (row.getValue("created_at") == null) {
            row.put("created_at", occurredAt != null ? toClickHouseDate(occurredAt) : toClickHouseDate(Instant.now().toString()));
        } else {
            row.put("created_at", toClickHouseDate(row.getString("created_at")));
        }
        return row;
    }

    /**
     * ClickHouse DateTime accepts {@code yyyy-MM-dd HH:mm:ss} but not the ISO
     * {@code T}/{@code Z} separators produced by the outbox envelope.
     */
    private static String toClickHouseDate(String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        String s = iso.trim().replace('T', ' ');
        if (s.endsWith("Z")) {
            s = s.substring(0, s.length() - 1);
        }
        // Keep only up to seconds (DateTime has second precision).
        int dot = s.indexOf('.');
        if (dot > 0) {
            s = s.substring(0, dot);
        }
        // ClickHouse DateTime needs HH:mm:ss — PG timestamps can arrive as
        // "yyyy-MM-dd HH:mm" (e.g. backfill) which must be padded to seconds.
        String[] dateTime = s.split(" ");
        if (dateTime.length == 2 && dateTime[1].matches("\\d{2}:\\d{2}")) {
            s = s + ":00";
        }
        return s;
    }

    /**
     * Periodically measures the outbox→ClickHouse pipeline lag: for each
     * assigned partition, (end offset − current position) summed. This is the
     * F6 outbox lag gauge — events published by the outbox relay still waiting
     * in Kafka to be consumed/written. Fully reactive: no blocking on the
     * event loop.
     */
    @Scheduled(every = "15s", delay = 30, delayUnit = TimeUnit.SECONDS)
    void probeKafkaLag() {
        if (consumer == null) {
            return;
        }
        Uni.createFrom().completionStage(consumer.assignment().toCompletionStage())
                .flatMap(partitions -> {
                    if (partitions == null || partitions.isEmpty()) {
                        metrics.setKafkaLag(0);
                        return Uni.createFrom().voidItem();
                    }
                    return lagFor(partitions);
                })
                .subscribe().with(
                        v -> { },
                        err -> log.debug("Kafka lag probe failed: {}", err.getMessage()));
    }

    private Uni<Void> lagFor(Set<TopicPartition> partitions) {
        return Uni.createFrom().completionStage(consumer.endOffsets(partitions).toCompletionStage())
                .flatMap(endOffsets -> {
                    List<Uni<Long>> lags = new java.util.ArrayList<>();
                    for (TopicPartition tp : partitions) {
                        Long end = endOffsets.get(tp);
                        if (end == null) {
                            continue;
                        }
                        lags.add(Uni.createFrom().completionStage(consumer.position(tp).toCompletionStage())
                                .map(pos -> Math.max(0, end - pos))
                                .onFailure().recoverWithItem(0L));
                    }
                    return Uni.combine().all().unis(lags)
                            .combinedWith(values -> {
                                long total = 0;
                                for (Object v : values) {
                                    total += ((Number) v).longValue();
                                }
                                metrics.setKafkaLag(total);
                                return total;
                            })
                            .replaceWithVoid();
                });
    }

    @PreDestroy
    void destroy() {
        if (consumer != null) {
            consumer.close()
                    .onSuccess(v -> log.info("StatsWriter consumer closed"))
                    .onFailure(err -> log.warn("Close error: {}", err.getMessage()));
        }
    }
}
