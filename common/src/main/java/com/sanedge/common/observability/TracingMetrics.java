package com.sanedge.common.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.ConfigProvider;

@ApplicationScoped
public class TracingMetrics {
    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter requestCounter;
    private final DoubleHistogram requestDurationHistogram;
    private final TextMapPropagator propagator;

    private static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("method");
    private static final AttributeKey<String> STATUS_KEY = AttributeKey.stringKey("status");

    // No-arg constructor required for CDI proxying
    protected TracingMetrics() {
        this.tracer = null;
        this.meter = null;
        this.requestCounter = null;
        this.requestDurationHistogram = null;
        this.propagator = null;
    }

    @Inject
    public TracingMetrics(OpenTelemetry openTelemetry) {
        String instrumentationName = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.application.name", String.class)
                .orElse("payment-service");

        this.tracer = openTelemetry.getTracer(instrumentationName);
        this.meter = openTelemetry.getMeter(instrumentationName);
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();

        this.requestCounter = meter.counterBuilder("requests_total")
                .setDescription("Total number of requests")
                .build();

        this.requestDurationHistogram = meter.histogramBuilder("request_duration_seconds")
                .setDescription("Request duration in seconds")
                .setUnit("s")
                .build();
    }

    public Tracer getTracer() {
        return tracer;
    }

    private static final TextMapGetter<Map<String, String>> MAP_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(@Nonnull Map<String, String> carrier) {
            return carrier.keySet();
        }

        @Override
        @Nullable
        public String get(@Nullable Map<String, String> carrier, @Nonnull String key) {
            return carrier != null ? carrier.get(key) : null;
        }
    };

    public void injectContext(@Nonnull Context context, @Nonnull Map<String, String> carrier) {
        propagator.inject(Objects.requireNonNull(context), Objects.requireNonNull(carrier), Map::put);
    }

    @Nonnull
    public Context extractContext(@Nonnull Map<String, String> carrier) {
        return Objects.requireNonNull(propagator.extract(
                Objects.requireNonNull(Context.current()),
                Objects.requireNonNull(carrier),
                Objects.requireNonNull(MAP_GETTER)));
    }

    public TracingContext startSpan(String operationName) {
        return startSpan(operationName, Attributes.empty());
    }

    public TracingContext startSpan(String operationName, Attributes attributes) {
        Instant startTime = Instant.now();

        Span span = tracer.spanBuilder(Objects.requireNonNull(operationName))
                .setSpanKind(SpanKind.INTERNAL)
                .setAllAttributes(Objects.requireNonNull(attributes))
                .startSpan();

        Context context = Context.current().with(span);

        return new TracingContext(context, startTime);
    }

    public <T> Uni<T> traceAndMeasure(String operationName, String method, Supplier<Uni<T>> supplier) {
        return traceAndMeasure(operationName, method, Attributes.empty(), supplier);
    }

    public <T> Uni<T> traceAndMeasure(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        TracingContext tracingContext = startSpan(operationName, attributes);

        try (Scope scope = tracingContext.getContext().makeCurrent()) {
            Uni<T> result = supplier.get();

            return result
                    .onItemOrFailure().invoke((res, err) -> {
                        if (err != null) {
                            completeSpanError(tracingContext, method, "Operation failed: " + err.getMessage());
                        } else {
                            completeSpanSuccess(tracingContext, method, "Operation completed successfully");
                        }
                    });
        } catch (Exception e) {
            completeSpanError(tracingContext, method, "Operation failed: " + e.getMessage());
            throw e;
        }
    }

    public <T> T traceAndMeasureSync(String operationName, String method, Supplier<T> supplier) {
        return traceAndMeasureSync(operationName, method, Attributes.empty(), supplier);
    }

    public <T> T traceAndMeasureSync(String operationName, String method, Attributes attributes, Supplier<T> supplier) {
        TracingContext tracingContext = startSpan(operationName, attributes);

        try (Scope scope = tracingContext.getContext().makeCurrent()) {
            T result = supplier.get();
            completeSpanSuccess(tracingContext, method, "Operation completed successfully");
            return result;
        } catch (Exception e) {
            completeSpanError(tracingContext, method, "Operation failed: " + e.getMessage());
            throw e;
        }
    }

    public void completeSpanSuccess(TracingContext tracingContext, String method, String message) {
        completeSpan(tracingContext, method, true, message);
    }

    public void completeSpanError(TracingContext tracingContext, String method, String errorMessage) {
        completeSpan(tracingContext, method, false, errorMessage);
    }

    private void completeSpan(TracingContext tracingContext, String method, boolean isSuccess, String message) {
        String status = isSuccess ? "SUCCESS" : "ERROR";
        double duration = Duration.between(tracingContext.getStartTime(), Instant.now()).toMillis() / 1000.0;

        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        span.addEvent("Operation completed", Attributes.builder()
                .put("status", status)
                .put("duration_secs", duration)
                .put("message", message)
                .build());

        if (isSuccess) {
            span.setStatus(StatusCode.OK);
        } else {
            span.setStatus(StatusCode.ERROR, message);
        }

        Attributes metricAttributes = Attributes.builder()
                .put(METHOD_KEY, method)
                .put(STATUS_KEY, status)
                .build();

        requestCounter.add(1, metricAttributes);
        requestDurationHistogram.record(duration, metricAttributes);

        span.end();
    }

    public static class TracingContext {
        private final Context context;
        private final Instant startTime;

        public TracingContext(Context context, Instant startTime) {
            this.context = context;
            this.startTime = startTime;
        }

        public Context getContext() {
            return context;
        }

        public Instant getStartTime() {
            return startTime;
        }
    }
}