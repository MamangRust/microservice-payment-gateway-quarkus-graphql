package com.sanedge.common.test;

import java.util.function.Supplier;

import com.sanedge.common.observability.TracingMetrics;

import io.opentelemetry.api.common.Attributes;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
public class MockTracingMetrics extends TracingMetrics {

    public MockTracingMetrics() {
        super();
    }

    @Override
    public <T> Uni<T> traceAndMeasure(String operationName, String method, Supplier<Uni<T>> supplier) {
        return supplier.get();
    }

    @Override
    public <T> Uni<T> traceAndMeasure(String operationName, String method, Attributes attributes,
            Supplier<Uni<T>> supplier) {
        return supplier.get();
    }

    @Override
    public <T> T traceAndMeasureSync(String operationName, String method, Supplier<T> supplier) {
        return supplier.get();
    }

    @Override
    public <T> T traceAndMeasureSync(String operationName, String method, Attributes attributes, Supplier<T> supplier) {
        return supplier.get();
    }

    @Override
    public void completeSpanSuccess(TracingContext tracingContext, String method, String message) {

    }

    @Override
    public void completeSpanError(TracingContext tracingContext, String method, String errorMessage) {

    }
}
