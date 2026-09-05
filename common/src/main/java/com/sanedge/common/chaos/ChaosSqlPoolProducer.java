package com.sanedge.common.chaos;

import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@ApplicationScoped
public class ChaosSqlPoolProducer {
  private static final Logger log = LoggerFactory.getLogger(ChaosSqlPoolProducer.class);

  @Inject
  BeanManager beanManager;

  @Inject
  ChaosManager chaosManager;

  @Inject
  Vertx vertx;

  private volatile Pool wrappedPool;

  @Produces
  @Alternative
  @Priority(1)
  @ApplicationScoped
  @Typed(Pool.class)
  public Pool producePool() {
    if (wrappedPool == null) {
      synchronized (this) {
        if (wrappedPool == null) {
          Set<Bean<?>> beans = beanManager.getBeans(Pool.class);
          Bean<?> originalBean = null;
          for (Bean<?> bean : beans) {
            if (bean.getBeanClass() != ChaosSqlPoolProducer.class) {
              originalBean = bean;
              break;
            }
          }
          if (originalBean == null) {
            log.error("❌ No active SQL Pool bean found to wrap for chaos engineering.");
            throw new IllegalStateException("No active SQL Pool bean found to wrap for chaos engineering. Ensure a reactive SQL client is configured.");
          }
          try {
            Pool originalPool = (Pool) beanManager.getReference(
                originalBean, Pool.class, beanManager.createCreationalContext(originalBean));
            wrappedPool = ChaosSqlProxy.wrap(originalPool, chaosManager, vertx);
            log.info("✅ Successfully wrapped database Pool with ChaosSqlProxy.");
          } catch (Exception e) {
            log.error("❌ Failed to wrap SQL Pool with ChaosSqlProxy: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to wrap database Pool with ChaosSqlProxy", e);
          }
        }
      }
    }
    return wrappedPool;
  }
}
