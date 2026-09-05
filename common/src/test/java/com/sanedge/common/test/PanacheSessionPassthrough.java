package com.sanedge.common.test;

import java.util.function.Supplier;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.smallrye.mutiny.Uni;

/**
 * JUnit 5 extension for reactive gRPC handler unit tests.
 *
 * <p>Handlers wrap service calls in {@code Panache.withSession(...)} /
 * {@code Panache.withTransaction(...)} and sometimes mark the current context
 * via {@code VertxContextSafetyToggle}. Those require a Vert.x context and an
 * active Hibernate Reactive session factory (both only available once Quarkus
 * is booted). For the fast Mockito unit tests that mock the domain services,
 * that session plumbing is irrelevant — this extension replaces the Panache
 * entry points with passthroughs so the mocked service {@code Uni} flows
 * through unchanged, and neutralises the context-safety toggle.</p>
 *
 * <p>Usage: add to the existing {@code @ExtendWith} on the test class, e.g.</p>
 *
 * <pre>{@code
 * @ExtendWith({ MockitoExtension.class, PanacheSessionPassthrough.class })
 * class MyHandlerTest {
 *     ...
 * }
 * }</pre>
 */
public class PanacheSessionPassthrough implements BeforeEachCallback, AfterEachCallback {

    private MockedStatic<Panache> panacheMock;
    private MockedStatic<VertxContextSafetyToggle> safetyToggleMock;

    @Override
    public void beforeEach(ExtensionContext context) {
        panacheMock = Mockito.mockStatic(Panache.class);
        // Panache.withSession(Supplier) — passthrough
        panacheMock.when(() -> Panache.withSession((Supplier<Uni<Object>>) Mockito.any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());
        // Panache.withTransaction(Supplier) — passthrough
        panacheMock.when(() -> Panache.withTransaction((Supplier<Uni<Object>>) Mockito.any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

        // VertxContextSafetyToggle.setCurrentContextSafe(boolean) is void: the
        // static mock already makes it a no-op, we just need the class mocked.
        safetyToggleMock = Mockito.mockStatic(VertxContextSafetyToggle.class);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (safetyToggleMock != null) {
            safetyToggleMock.close();
        }
        if (panacheMock != null) {
            panacheMock.close();
        }
    }
}
