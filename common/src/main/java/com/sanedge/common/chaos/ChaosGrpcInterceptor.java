package com.sanedge.common.chaos;

import io.grpc.*;
import io.quarkus.grpc.GlobalInterceptor;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GlobalInterceptor
@ApplicationScoped
public class ChaosGrpcInterceptor implements ServerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ChaosGrpcInterceptor.class);

    @Inject
    ChaosManager chaosManager;

    @Inject
    Vertx vertx;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String fullMethod = call.getMethodDescriptor().getFullMethodName();
        // format: "PackageName.ServiceName/MethodName"

        ChaosPolicy policy = chaosManager.evaluate("grpc", fullMethod);

        if (policy == null) {
            // coba match service-level wildcard
            String serviceName = fullMethod.split("/")[0];
            policy = chaosManager.evaluate("grpc", serviceName + "/*");
        }

        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
            log.info("🔥 Injecting gRPC chaos [Policy: {}] for method: {}", 
                policy.getName(), fullMethod);

            final ChaosPolicy finalPolicy = policy;

            if (policy.getLatencyMs() > 0) {
                long delay = policy.getLatencyMs();
                vertx.setTimer(delay, id -> {
                    Status status = resolveGrpcStatus(finalPolicy);
                    call.close(status, new Metadata());
                });
                return new ServerCall.Listener<ReqT>() {}; // no-op listener
            } else {
                Status status = resolveGrpcStatus(policy);
                call.close(status, new Metadata());
                return new ServerCall.Listener<ReqT>() {}; // no-op listener
            }
        }

        return next.startCall(call, headers);
    }

    private Status resolveGrpcStatus(ChaosPolicy policy) {
        String grpcStatus = policy.getGrpcStatus();
        String msg = policy.getErrorMessage() != null 
            ? policy.getErrorMessage() 
            : "Chaos fault injected";

        if (grpcStatus == null) {
            return Status.UNAVAILABLE.withDescription(msg);
        }

        return switch (grpcStatus.toUpperCase()) {
            case "UNAVAILABLE"        -> Status.UNAVAILABLE.withDescription(msg);
            case "DEADLINE_EXCEEDED"  -> Status.DEADLINE_EXCEEDED.withDescription(msg);
            case "INTERNAL"           -> Status.INTERNAL.withDescription(msg);
            case "RESOURCE_EXHAUSTED" -> Status.RESOURCE_EXHAUSTED.withDescription(msg);
            case "NOT_FOUND"          -> Status.NOT_FOUND.withDescription(msg);
            case "PERMISSION_DENIED"  -> Status.PERMISSION_DENIED.withDescription(msg);
            default                   -> Status.UNKNOWN.withDescription(msg);
        };
    }
}
