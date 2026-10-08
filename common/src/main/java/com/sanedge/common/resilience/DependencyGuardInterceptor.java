package com.sanedge.common.resilience;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.Deadline;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GlobalInterceptor;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

/**
 * Padanan Java dari {@code pkg/resilience/client_interceptor.go}
 * ({@code DependencyGuardInterceptor}), dibangun di atas SmallRye Fault
 * Tolerance (ekstensi Quarkus yang sudah dipakai adapter di
 * {@code common/adapter}).
 *
 * <p>Untuk setiap full method name ({@code "/package.Service/Method"})
 * dibuat satu {@link Guard} berisi circuit breaker + bulkhead + timeout.
 * Karena setiap {@code Guard} memegang instance stateful-nya sendiri, tiap
 * dependency punya breaker dan bulkhead terpisah: satu dependency yang lambat
 * atau gagal tidak bisa menghabiskan resource pemanggil atau membuka sirkuit
 * untuk service lain.
 *
 * <p>Guard dipasang ke semua injected gRPC client lewat
 * {@link GlobalInterceptor}. Aksi gRPC bersifat asynchronous, jadi guard
 * memakai varian {@link CompletionStage}: call dianggap selesai saat
 * {@code CompletionStage} selesai, sehingga bulkhead menahan permit selama
 * call berlangsung dan circuit breaker mencatat hasil akhirnya.
 *
 * <p>Hanya kegagalan level transport (Unavailable / DeadlineExceeded / Aborted /
 * ResourceExhausted / timeout) yang dihitung ke circuit breaker; error bisnis
 * (not found, validasi, dsb) diperlakukan sebagai sukses, sama seperti Go.
 */
@GlobalInterceptor
@ApplicationScoped
public class DependencyGuardInterceptor implements ClientInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DependencyGuardInterceptor.class);

    @Inject
    DependencyGuardConfig config;

    private final ConcurrentMap<String, Guard> guards = new ConcurrentHashMap<>();

    /** Seam pengujian; di produksi selalu {@link #buildGuard(String)}. */
    Function<String, Guard> guardFactory = this::buildGuard;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {

        String fullMethod = method.getFullMethodName();
        Guard guard = guards.computeIfAbsent(fullMethod, guardFactory);
        return new GuardedClientCall<>(next.newCall(method, applyDeadline(callOptions)), guard);
    }

    Guard buildGuard(String fullMethod) {
        return Guard.create()
                .withDescription(fullMethod)
                .withCircuitBreaker()
                .when(DependencyGuardInterceptor::isTransportFailure)
                .requestVolumeThreshold((int) config.failureThreshold())
                .delay(config.openTimeoutSeconds(), ChronoUnit.SECONDS)
                .done()
                .withBulkhead()
                .limit(config.maxConcurrent())
                .done()
                .withTimeout()
                .duration(config.callTimeout().toMillis(), ChronoUnit.MILLIS)
                .done()
                .build();
    }

    /**
     * Memasang deadline per-call, tetapi tidak pernah memperpanjang deadline
     * yang sudah ada (mirip {@code context.WithTimeout} di Go: ambil yang lebih
     * pendek). Deadline ini yang membatalkan RPC di level transport saat
     * timeout.
     */
    private CallOptions applyDeadline(CallOptions callOptions) {
        long timeoutMs = config.callTimeout().toMillis();
        Deadline existing = callOptions.getDeadline();
        if (existing == null || existing.timeRemaining(TimeUnit.MILLISECONDS) > timeoutMs) {
            return callOptions.withDeadline(Deadline.after(timeoutMs, TimeUnit.MILLISECONDS));
        }
        return callOptions;
    }

    /** Hanya kegagalan transport yang dihitung sebagai failure circuit breaker. */
    static boolean isTransportFailure(Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof TimeoutException) {
            return true;
        }
        if (cause instanceof StatusRuntimeException sre) {
            return isTransportStatus(sre.getStatus());
        }
        if (cause instanceof StatusException se) {
            return isTransportStatus(se.getStatus());
        }
        return false;
    }

    private static boolean isTransportStatus(Status status) {
        return switch (status.getCode()) {
            case UNAVAILABLE, DEADLINE_EXCEEDED, ABORTED, RESOURCE_EXHAUSTED -> true;
            default -> false;
        };
    }

    /** Memetakan kegagalan guard/koneksi ke status gRPC yang setara. */
    static Status toStatus(Throwable error) {
        Throwable cause = unwrap(error);
        if (cause instanceof StatusRuntimeException sre) {
            return sre.getStatus();
        }
        if (cause instanceof StatusException se) {
            return se.getStatus();
        }
        if (cause instanceof TimeoutException) {
            return Status.DEADLINE_EXCEEDED.withDescription("dependency call timed out");
        }
        if (cause instanceof CircuitBreakerOpenException) {
            return Status.UNAVAILABLE.withDescription("dependency circuit open; failing fast");
        }
        if (cause instanceof BulkheadException) {
            return Status.RESOURCE_EXHAUSTED.withDescription("dependency bulkhead full");
        }
        return Status.UNKNOWN.withDescription(cause.getClass().getSimpleName() + ": " + cause.getMessage());
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Membungkus {@link ClientCall} agar eksekusi lewat {@link Guard}. Pesan dan
     * header diteruskan apa adanya; {@code onClose} baru diteruskan setelah
     * {@code CompletionStage} guard selesai (sukses, ditolak bulkhead, sirkuit
     * terbuka, atau timeout).
     */
    static final class GuardedClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {

        private final ClientCall<ReqT, RespT> delegate;
        private final Guard guard;

        GuardedClientCall(ClientCall<ReqT, RespT> delegate, Guard guard) {
            this.delegate = delegate;
            this.guard = guard;
        }

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
            AtomicReference<Metadata> trailers = new AtomicReference<>();

            Callable<CompletionStage<RespT>> action = () -> {
                CompletableFuture<RespT> future = new CompletableFuture<>();
                delegate.start(new Listener<RespT>() {
                    @Override
                    public void onMessage(RespT message) {
                        listener.onMessage(message);
                    }

                    @Override
                    public void onHeaders(Metadata responseHeaders) {
                        listener.onHeaders(responseHeaders);
                    }

                    @Override
                    public void onReady() {
                        listener.onReady();
                    }

                    @Override
                    public void onClose(Status status, Metadata responseTrailers) {
                        trailers.set(responseTrailers);
                        if (status.isOk()) {
                            future.complete(null);
                        } else {
                            future.completeExceptionally(status.asRuntimeException(responseTrailers));
                        }
                    }
                }, headers);
                return future;
            };

            try {
                CompletionStage<RespT> stage = guard.call(action, new TypeLiteral<CompletionStage<RespT>>() {
                });
                stage.whenComplete((value, error) -> {
                    Metadata responseTrailers = trailers.get();
                    listener.onClose(
                            error == null ? Status.OK : toStatus(error),
                            responseTrailers != null ? responseTrailers : new Metadata());
                });
            } catch (Exception e) {
                log.debug("Dependency guard rejected call: {}", e.toString());
                listener.onClose(toStatus(e), new Metadata());
            }
        }

        @Override
        public void request(int numMessages) {
            delegate.request(numMessages);
        }

        @Override
        public void cancel(String message, Throwable cause) {
            delegate.cancel(message, cause);
        }

        @Override
        public void halfClose() {
            delegate.halfClose();
        }

        @Override
        public void sendMessage(ReqT message) {
            delegate.sendMessage(message);
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }
    }
}
