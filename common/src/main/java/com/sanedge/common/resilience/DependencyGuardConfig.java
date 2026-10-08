package com.sanedge.common.resilience;

import java.time.Duration;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Tuning untuk {@link DependencyGuardInterceptor}. Nilai default meniru
 * konstanta di {@code pkg/resilience/client_interceptor.go}:
 * 5 kegagalan membuka breaker, 30s sampai half-open, maks 100 call konkuren,
 * deadline per-call 3s.
 *
 * <p>Contoh override:
 * <pre>
 * resilience.dependency-guard.failure-threshold=5
 * resilience.dependency-guard.open-timeout-seconds=30
 * resilience.dependency-guard.max-concurrent=100
 * resilience.dependency-guard.call-timeout=3s
 * </pre>
 */
@ConfigMapping(prefix = "resilience.dependency-guard")
public interface DependencyGuardConfig {

    @WithDefault("5")
    long failureThreshold();

    @WithDefault("30")
    long openTimeoutSeconds();

    @WithDefault("100")
    int maxConcurrent();

    @WithDefault("3s")
    Duration callTimeout();
}
