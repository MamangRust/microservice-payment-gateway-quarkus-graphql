package com.sanedge.card.scheduler;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanedge.card.service.BillingEngineService;

import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class BillingScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingScheduler.class);

    private static final long ONE_HOUR_MS = 3600_000L;

    @Inject
    Vertx vertx;

    @Inject
    BillingEngineService billingEngineService;

    private Long timerId;

    @PostConstruct
    void init() {
        // Start billing cycle check every hour
        timerId = vertx.setPeriodic(ONE_HOUR_MS, id -> {
            int currentDay = LocalDate.now().getDayOfMonth();
            log.debug("BillingScheduler: triggering billing cycle for day {}", currentDay);

            billingEngineService.triggerBillingCycle(currentDay)
                    .subscribe().with(
                            result -> log.info("BillingScheduler: cycle completed, {} statements processed",
                                    result.data() != null ? result.data() : 0),
                            err -> log.error("BillingScheduler: cycle failed", err));
        });

        log.info("✅ BillingScheduler started (every 1 hour)");
    }

    @PreDestroy
    void destroy() {
        if (timerId != null) {
            vertx.cancelTimer(timerId);
            log.info("✅ BillingScheduler stopped.");
        }
    }
}
