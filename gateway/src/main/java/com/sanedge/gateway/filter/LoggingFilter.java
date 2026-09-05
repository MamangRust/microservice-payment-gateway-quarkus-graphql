package com.sanedge.gateway.filter;

import io.quarkus.logging.Log;
import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;
import org.slf4j.MDC;

@ApplicationScoped
public class LoggingFilter {

    private static final String START_TIME = "start-time";
    private static final String CORRELATION_ID = "correlation-id";

    @RouteFilter(100)
    public void filter(RoutingContext rc) {
        long startTime = System.currentTimeMillis();
        rc.put(START_TIME, startTime);

        String correlationId = rc.request().getHeader("X-Correlation-ID");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        rc.put(CORRELATION_ID, correlationId);
        MDC.put("CorrelationId", correlationId);

        String method = rc.request().method().name();
        String path = rc.request().path();
        Log.infof("Incoming request: %s %s [Correlation ID: %s]", method, path, correlationId);

        rc.addBodyEndHandler(v -> {
            Long start = rc.get(START_TIME);
            String corrId = rc.get(CORRELATION_ID);
            
            long duration = start != null ? (System.currentTimeMillis() - start) : 0;
            int status = rc.response().getStatusCode();

            Log.infof("Outgoing response: %s %s -> Status: %d [Duration: %dms, Correlation ID: %s]",
                    method, path, status, duration, corrId);

            if (corrId != null) {
                rc.response().headers().set("X-Correlation-ID", corrId);
            }
            MDC.clear();
        });

        rc.next();
    }
}
