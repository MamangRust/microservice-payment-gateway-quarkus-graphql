package com.sanedge.statswriter;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Periodically flushes pending ClickHouse batches and evicts expired dedup
 * entries (mirror of the Go flush ticker, 5s).
 */
@ApplicationScoped
public class FlushScheduler {

    private static final Logger log = LoggerFactory.getLogger(FlushScheduler.class);

    @Inject
    ClickHouseBatchWriter batchWriter;

    @Inject
    EventDedup dedup;

    @Inject
    StatsWriterMetrics metrics;

    @ConfigProperty(name = "stats.flush-interval-seconds", defaultValue = "5")
    int flushIntervalSeconds;

    @Scheduled(every = "5s", delay = 10, delayUnit = TimeUnit.SECONDS)
    void flush() {
        batchWriter.flushAll()
                .subscribe().with(
                        v -> {
                            metrics.setBatchPending(batchWriter.pendingCount());
                            log.debug("Flushed pending batches (pending={})", batchWriter.pendingCount());
                        },
                        err -> log.warn("Flush error: {}", err.getMessage()));
        dedup.purgeExpired();
    }
}
