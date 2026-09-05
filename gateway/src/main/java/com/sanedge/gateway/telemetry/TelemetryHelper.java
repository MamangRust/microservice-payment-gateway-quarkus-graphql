package com.sanedge.gateway.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.function.Supplier;

@ApplicationScoped
public class TelemetryHelper {

    private final Tracer tracer;
    private final LongCounter requestsTotal;
    private final DoubleHistogram requestDurationSeconds;

    @Inject
    public TelemetryHelper(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("gateway-service", "1.0.0");
        Meter meter = openTelemetry.getMeter("gateway-service");

        this.requestsTotal = meter.counterBuilder("gateway_requests_total")
                .setDescription("Total number of gateway requests")
                .build();

        this.requestDurationSeconds = meter.histogramBuilder("gateway_request_duration_seconds")
                .setDescription("Gateway request duration in seconds")
                .setUnit("s")
                .build();
    }

    public <T> Uni<T> traceAndMetric(String operationName, Uni<T> uni) {
        long startTime = System.currentTimeMillis();
        Span span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("service.name", "gateway")
                .setAttribute("operation", operationName)
                .startSpan();

        return uni
                .onItem().invoke(item -> {
                    span.setStatus(StatusCode.OK);
                    requestsTotal.add(1, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName,
                            AttributeKey.stringKey("status"), "success"
                    ));
                })
                .onFailure().invoke(throwable -> {
                    span.recordException(throwable);
                    span.setStatus(StatusCode.ERROR, throwable.getMessage());
                    requestsTotal.add(1, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName,
                            AttributeKey.stringKey("status"), "failed",
                            AttributeKey.stringKey("error_type"), throwable.getClass().getSimpleName()
                    ));
                })
                .eventually(() -> {
                    span.end();
                    double duration = (System.currentTimeMillis() - startTime) / 1000.0;
                    requestDurationSeconds.record(duration, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName
                    ));
                });
    }

    public <T> Uni<T> traceAndMetric(String operationName, Supplier<Uni<T>> supplier) {
        long startTime = System.currentTimeMillis();
        Span span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("service.name", "gateway")
                .setAttribute("operation", operationName)
                .startSpan();

        Uni<T> uni;
        try {
            uni = supplier.get();
        } catch (Throwable throwable) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
            requestsTotal.add(1, Attributes.of(
                    AttributeKey.stringKey("operation"), operationName,
                    AttributeKey.stringKey("status"), "failed",
                    AttributeKey.stringKey("error_type"), throwable.getClass().getSimpleName()
            ));
            span.end();
            double duration = (System.currentTimeMillis() - startTime) / 1000.0;
            requestDurationSeconds.record(duration, Attributes.of(
                    AttributeKey.stringKey("operation"), operationName
            ));
            return Uni.createFrom().failure(throwable);
        }

        return uni
                .onItem().invoke(item -> {
                    span.setStatus(StatusCode.OK);
                    requestsTotal.add(1, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName,
                            AttributeKey.stringKey("status"), "success"
                    ));
                })
                .onFailure().invoke(throwable -> {
                    span.recordException(throwable);
                    span.setStatus(StatusCode.ERROR, throwable.getMessage());
                    requestsTotal.add(1, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName,
                            AttributeKey.stringKey("status"), "failed",
                            AttributeKey.stringKey("error_type"), throwable.getClass().getSimpleName()
                    ));
                })
                .eventually(() -> {
                    span.end();
                    double duration = (System.currentTimeMillis() - startTime) / 1000.0;
                    requestDurationSeconds.record(duration, Attributes.of(
                            AttributeKey.stringKey("operation"), operationName
                    ));
                });
    }
}
