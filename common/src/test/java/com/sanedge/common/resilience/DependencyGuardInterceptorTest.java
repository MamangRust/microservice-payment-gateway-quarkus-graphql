package com.sanedge.common.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.smallrye.faulttolerance.api.Guard;
import jakarta.enterprise.util.TypeLiteral;

class DependencyGuardInterceptorTest {

    private static final MethodDescriptor.Marshaller<String> STRING_MARSHALLER = new MethodDescriptor.Marshaller<>() {
        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    };

    private static final MethodDescriptor<String, String> METHOD = MethodDescriptor
            .<String, String>newBuilder()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName("test.Service/Method")
            .setRequestMarshaller(STRING_MARSHALLER)
            .setResponseMarshaller(STRING_MARSHALLER)
            .build();

    private DependencyGuardInterceptor interceptor;
    private RecordingChannel channel;

    @BeforeEach
    void setUp() {
        interceptor = new DependencyGuardInterceptor();
        interceptor.config = config(10, 5, 30, 3000);
        interceptor.guardFactory = method -> passthroughGuard();
        channel = new RecordingChannel();
    }

    @Test
    void appliesPerCallDeadline() {
        channel.delegate = new RecordingClientCall<>();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel);

        assertThat(channel.lastOptions.getDeadline()).isNotNull();
    }

    @Test
    void keepsShorterExistingDeadline() {
        channel.delegate = new RecordingClientCall<>();
        CallOptions withShorterDeadline = CallOptions.DEFAULT.withDeadlineAfter(1, TimeUnit.MILLISECONDS);

        interceptor.interceptCall(METHOD, withShorterDeadline, channel);

        assertThat(channel.lastOptions.getDeadline()).isEqualTo(withShorterDeadline.getDeadline());
    }

    @Test
    void forwardsSuccessfulResponse() {
        RecordingClientCall<String, String> delegate = new RecordingClientCall<>();
        channel.delegate = delegate;
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());
        delegate.emitMessage("hello");
        delegate.complete(Status.OK);

        assertThat(listener.messages).containsExactly("hello");
        assertThat(listener.status.getCode()).isEqualTo(Status.Code.OK);
    }

    @Test
    void mapsTransportFailureStatus() {
        RecordingClientCall<String, String> delegate = new RecordingClientCall<>();
        channel.delegate = delegate;
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());
        delegate.complete(Status.UNAVAILABLE);

        assertThat(listener.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void passesThroughBusinessErrorStatus() {
        RecordingClientCall<String, String> delegate = new RecordingClientCall<>();
        channel.delegate = delegate;
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());
        delegate.complete(Status.NOT_FOUND);

        assertThat(listener.status.getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void mapsBulkheadRejection() {
        interceptor.guardFactory = method -> throwingGuard(new BulkheadException("full"));
        channel.delegate = new RecordingClientCall<>();
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());

        assertThat(listener.status.getCode()).isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
    }

    @Test
    void mapsCircuitBreakerOpenRejection() {
        interceptor.guardFactory = method -> throwingGuard(new CircuitBreakerOpenException("open"));
        channel.delegate = new RecordingClientCall<>();
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());

        assertThat(listener.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void mapsAsyncGuardRejection() {
        interceptor.guardFactory = method -> failedStageGuard(new CircuitBreakerOpenException("open"));
        channel.delegate = new RecordingClientCall<>();
        TestListener listener = new TestListener();

        interceptor.interceptCall(METHOD, CallOptions.DEFAULT, channel).start(listener, new Metadata());

        assertThat(listener.status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void classifiesTransportFailuresOnly() {
        assertThat(DependencyGuardInterceptor.isTransportFailure(Status.UNAVAILABLE.asRuntimeException())).isTrue();
        assertThat(DependencyGuardInterceptor.isTransportFailure(Status.DEADLINE_EXCEEDED.asRuntimeException()))
                .isTrue();
        assertThat(DependencyGuardInterceptor.isTransportFailure(Status.ABORTED.asRuntimeException())).isTrue();
        assertThat(DependencyGuardInterceptor.isTransportFailure(Status.RESOURCE_EXHAUSTED.asRuntimeException()))
                .isTrue();
        assertThat(DependencyGuardInterceptor.isTransportFailure(new TimeoutException())).isTrue();

        assertThat(DependencyGuardInterceptor.isTransportFailure(Status.NOT_FOUND.asRuntimeException())).isFalse();
        assertThat(DependencyGuardInterceptor.isTransportFailure(new IllegalStateException("boom"))).isFalse();
        assertThat(DependencyGuardInterceptor.isTransportFailure(null)).isFalse();
    }

    @Test
    void mapsTimeoutToDeadlineExceeded() {
        Status status = DependencyGuardInterceptor.toStatus(new TimeoutException("late"));

        assertThat(status.getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
    }

    private static DependencyGuardConfig config(int maxConcurrent, long threshold, long timeoutSeconds,
            long callTimeoutMs) {
        return new DependencyGuardConfig() {
            @Override
            public long failureThreshold() {
                return threshold;
            }

            @Override
            public long openTimeoutSeconds() {
                return timeoutSeconds;
            }

            @Override
            public int maxConcurrent() {
                return maxConcurrent;
            }

            @Override
            public Duration callTimeout() {
                return Duration.ofMillis(callTimeoutMs);
            }
        };
    }

    private static Guard passthroughGuard() {
        return new Guard() {
            @Override
            public <T> T call(Callable<T> action, Class<T> type) throws Exception {
                return action.call();
            }

            @Override
            public <T> T call(Callable<T> action, TypeLiteral<T> type) throws Exception {
                return action.call();
            }

            @Override
            public <T> T get(Supplier<T> action, Class<T> type) {
                return action.get();
            }

            @Override
            public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
                return action.get();
            }
        };
    }

    private static Guard throwingGuard(RuntimeException failure) {
        return new Guard() {
            @Override
            public <T> T call(Callable<T> action, Class<T> type) {
                throw failure;
            }

            @Override
            public <T> T call(Callable<T> action, TypeLiteral<T> type) {
                throw failure;
            }

            @Override
            public <T> T get(Supplier<T> action, Class<T> type) {
                throw failure;
            }

            @Override
            public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
                throw failure;
            }
        };
    }

    private static Guard failedStageGuard(Throwable failure) {
        return new Guard() {
            @SuppressWarnings("unchecked")
            @Override
            public <T> T call(Callable<T> action, Class<T> type) {
                return (T) CompletableFuture.failedFuture(failure);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T call(Callable<T> action, TypeLiteral<T> type) {
                return (T) CompletableFuture.failedFuture(failure);
            }

            @Override
            public <T> T get(Supplier<T> action, Class<T> type) {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T get(Supplier<T> action, TypeLiteral<T> type) {
                throw new UnsupportedOperationException();
            }
        };
    }

    static final class RecordingChannel extends Channel {

        ClientCall<?, ?> delegate;
        CallOptions lastOptions;

        @SuppressWarnings("unchecked")
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions) {
            this.lastOptions = callOptions;
            return (ClientCall<ReqT, RespT>) delegate;
        }

        @Override
        public String authority() {
            return "test";
        }
    }

    static final class RecordingClientCall<ReqT, RespT> extends ClientCall<ReqT, RespT> {

        ClientCall.Listener<RespT> listener;
        boolean started;

        @Override
        public void start(Listener<RespT> listener, Metadata headers) {
            this.listener = listener;
            this.started = true;
        }

        void emitMessage(RespT message) {
            listener.onMessage(message);
        }

        void complete(Status status) {
            listener.onClose(status, new Metadata());
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }

        @Override
        public void halfClose() {
        }

        @Override
        public void sendMessage(ReqT message) {
        }

        @Override
        public boolean isReady() {
            return true;
        }
    }

    static final class TestListener extends ClientCall.Listener<String> {

        final List<String> messages = new ArrayList<>();
        Status status;

        @Override
        public void onMessage(String message) {
            messages.add(message);
        }

        @Override
        public void onClose(Status status, Metadata trailers) {
            this.status = status;
        }
    }
}
