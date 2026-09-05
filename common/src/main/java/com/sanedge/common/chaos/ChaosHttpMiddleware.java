package com.sanedge.common.chaos;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChaosHttpMiddleware implements Handler<RoutingContext> {
  private static final Logger log = LoggerFactory.getLogger(ChaosHttpMiddleware.class);

  private final ChaosManager manager;

  public ChaosHttpMiddleware(ChaosManager manager) {
    this.manager = manager;
  }

  @Override
  public void handle(RoutingContext ctx) {
    String path = ctx.request().path();
    String method = ctx.request().method().name();
    String fullTarget = method + ":" + path;

    // Check for ignored paths (like health check or chaos dashboard itself)
    if (path.equals("/health") || path.startsWith("/api/chaos")) {
      ctx.next();
      return;
    }

    ChaosPolicy temp = manager.evaluate("http", fullTarget);
    if (temp == null) {
      temp = manager.evaluate("http", path);
    }
    final ChaosPolicy policy = temp;

    if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
      log.info("🔥 Injecting HTTP chaos [Policy: {}] to request: {} {}", policy.getName(), method, path);

      if (policy.isDropConnection()) {
        log.warn("🔌 Closing connection abruptly (drop_connection) for: {} {}", method, path);
        ctx.request().connection().close();
        return;
      }

      long latency = policy.getLatencyMs();
      if (latency > 0) {
        ctx.vertx().setTimer(latency, timerId -> executeHttpFault(ctx, policy));
      } else {
        executeHttpFault(ctx, policy);
      }
    } else {
      ctx.next();
    }
  }

  private void executeHttpFault(RoutingContext ctx, ChaosPolicy policy) {
    int code = policy.getErrorCode() != 0 ? policy.getErrorCode() : 503;
    String body = policy.getErrorBody() != null ? policy.getErrorBody() : "{\"error\":\"chaos_fault\",\"message\":\"Simulated chaos error\"}";
    
    ctx.response()
        .setStatusCode(code)
        .putHeader("Content-Type", "application/json")
        .end(body);
  }
}
