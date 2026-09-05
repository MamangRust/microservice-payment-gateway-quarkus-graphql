package com.sanedge.common.chaos;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class ChaosSqlProxy implements InvocationHandler {
  private static final Logger log = LoggerFactory.getLogger(ChaosSqlProxy.class);

  private final Object delegate;
  private final ChaosManager manager;
  private final Vertx vertx;

  public ChaosSqlProxy(Object delegate, ChaosManager manager, Vertx vertx) {
    this.delegate = delegate;
    this.manager = manager;
    this.vertx = vertx;
  }

  public static Pool wrap(Pool pool, ChaosManager manager, Vertx vertx) {
    List<Class<?>> interfaces = new ArrayList<>();
    interfaces.add(Pool.class);
    try {
      Class<?> pgPoolClass = Class.forName("io.vertx.pgclient.PgPool");
      if (pgPoolClass.isInstance(pool)) {
        interfaces.add(pgPoolClass);
      }
    } catch (ClassNotFoundException e) {
      // Ignored if pg client is not on classpath
    }

    return (Pool) Proxy.newProxyInstance(
        Pool.class.getClassLoader(),
        interfaces.toArray(new Class<?>[0]),
        new ChaosSqlProxy(pool, manager, vertx));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    String methodName = method.getName();

    if ("query".equals(methodName) && args != null && args.length == 1 && args[0] instanceof String) {
      String sql = (String) args[0];
      Object result = method.invoke(delegate, args);
      if (result instanceof Query) {
        return wrapQuery((Query<?>) result, sql);
      }
    }

    if ("preparedQuery".equals(methodName) && args != null && args.length == 1 && args[0] instanceof String) {
      String sql = (String) args[0];
      Object result = method.invoke(delegate, args);
      if (result instanceof PreparedQuery) {
        return wrapPreparedQuery((PreparedQuery<?>) result, sql);
      }
    }

    try {
      return method.invoke(delegate, args);
    } catch (Exception e) {
      throw e.getCause() != null ? e.getCause() : e;
    }
  }

  private Object wrapQuery(Query<?> query, String sql) {
    return Proxy.newProxyInstance(
        Query.class.getClassLoader(),
        new Class<?>[] { Query.class },
        new QueryInvocationHandler(query, sql, manager, vertx));
  }

  private Object wrapPreparedQuery(PreparedQuery<?> preparedQuery, String sql) {
    return Proxy.newProxyInstance(
        PreparedQuery.class.getClassLoader(),
        new Class<?>[] { PreparedQuery.class },
        new QueryInvocationHandler(preparedQuery, sql, manager, vertx));
  }

  private static class QueryInvocationHandler implements InvocationHandler {
    private final Object queryDelegate;
    private final String sql;
    private final ChaosManager manager;
    private final Vertx vertx;

    public QueryInvocationHandler(Object queryDelegate, String sql, ChaosManager manager, Vertx vertx) {
      this.queryDelegate = queryDelegate;
      this.sql = sql;
      this.manager = manager;
      this.vertx = vertx;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      String methodName = method.getName();

      if ("execute".equals(methodName)) {
        ChaosPolicy temp = manager.evaluate("sql", sql);
        if (temp == null) {
          temp = findTableMatch(sql);
        }
        final ChaosPolicy policy = temp;

        if (policy != null && policy.isEnabled() && Math.random() < policy.getErrorChance()) {
          log.info("🔥 Injecting SQL chaos [Policy: {}] for query: {}", policy.getName(),
              sql.trim().replaceAll("\\s+", " "));

          if (policy.getLatencyMs() > 0) {
            long delay = policy.getLatencyMs();
            Promise<Object> promise = Promise.promise();
            boolean shouldFail = policy.getErrorMessage() != null || policy.getErrorCode() != 0;

            vertx.setTimer(delay, id -> {
              if (shouldFail) {
                promise.fail(new RuntimeException(
                    policy.getErrorMessage() != null ? policy.getErrorMessage() : "Simulated database deadlock/error"));
              } else {
                try {
                  Future<?> fut = (Future<?>) method.invoke(queryDelegate, args);
                  fut.onComplete(ar -> {
                    if (ar.succeeded()) {
                      promise.complete(ar.result());
                    } else {
                      promise.fail(ar.cause());
                    }
                  });
                } catch (Exception e) {
                  promise.fail(e.getCause() != null ? e.getCause() : e);
                }
              }
            });
            return promise.future();
          } else if (policy.getErrorMessage() != null || policy.getErrorCode() != 0) {
            return Future.failedFuture(new RuntimeException(
                policy.getErrorMessage() != null ? policy.getErrorMessage() : "Simulated database deadlock/error"));
          }
        }
      }

      try {
        return method.invoke(queryDelegate, args);
      } catch (Exception e) {
        throw e.getCause() != null ? e.getCause() : e;
      }
    }

    private ChaosPolicy findTableMatch(String sql) {
      for (ChaosPolicy policy : manager.getPolicies()) {
        if (policy.isEnabled() && "sql".equalsIgnoreCase(policy.getType())) {
          String target = policy.getTarget();
          if (sql.toLowerCase().contains(target.toLowerCase())) {
            return policy;
          }
        }
      }
      return null;
    }
  }
}
