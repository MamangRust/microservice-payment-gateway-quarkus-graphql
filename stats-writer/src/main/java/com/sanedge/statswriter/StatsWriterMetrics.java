package com.sanedge.statswriter;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OTel metrics for the stats pipeline (F6 hardening).
 *
 * <ul>
 *   <li>{@code stats_writer_kafka_lag} — gauge: sum of (end offset − current
 *       position) over the assigned partitions. This is the outbox→ClickHouse
 *       pipeline lag: events published by the F3 outbox relay that are still
 *       waiting in Kafka to be consumed.</li>
 *   <li>{@code stats_writer_batch_pending} — gauge: rows buffered in the
 *       batch writer awaiting the next ClickHouse flush.</li>
 *   <li>{@code stats_writer_events_consumed_total} — counter: records consumed
 *       from the stats topics (post-dedup).</li>
 *   <li>{@code stats_writer_flush_errors_total} — counter: ClickHouse flush
 *       failures (re-buffered for retry, at-least-once).</li>
 * </ul>
 */
@ApplicationScoped
public class StatsWriterMetrics {

    private static final Logger log = LoggerFactory.getLogger(StatsWriterMetrics.class);

    private final LongCounter eventsConsumed;
    private final LongCounter flushErrors;
    private final AtomicLong kafkaLag = new AtomicLong();
    private final AtomicLong batchPending = new AtomicLong();

    @Inject
    public StatsWriterMetrics(OpenTelemetry openTelemetry) {
        Meter meter = openTelemetry.getMeter("stats-writer");

        this.eventsConsumed = meter.counterBuilder("stats_writer_events_consumed_total")
                .setDescription("Total stats events consumed from Kafka (post-dedup)")
                .build();

        this.flushErrors = meter.counterBuilder("stats_writer_flush_errors_total")
                .setDescription("ClickHouse flush failures (re-buffered for retry)")
                .build();

        meter.gaugeBuilder("stats_writer_kafka_lag")
                .setDescription("Outbox->ClickHouse pipeline lag: Kafka consumer lag summed over partitions")
                .ofLongs()
                .buildWithCallback(obs -> obs.record(kafkaLag.get()));

        meter.gaugeBuilder("stats_writer_batch_pending")
                .setDescription("Rows buffered in batch writer awaiting ClickHouse flush")
                .ofLongs()
                .buildWithCallback(obs -> obs.record(batchPending.get()));
    }

    public void recordEventConsumed() {
        eventsConsumed.add(1);
    }

    public void recordFlushError() {
        flushErrors.add(1);
    }

    public void setKafkaLag(long lag) {
        kafkaLag.set(Math.max(0, lag));
    }

    public void setBatchPending(int pending) {
        batchPending.set(Math.max(0, pending));
    }

    public long kafkaLag() {
        return kafkaLag.get();
    }
}
