package com.sanedge.common.chaos;

import io.vertx.core.Vertx;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Startup
@ApplicationScoped
public class ChaosManager {
  private static final Logger log = LoggerFactory.getLogger(ChaosManager.class);

  private static final String DEFAULT_CONFIG_PATH = "chaos.yaml";
  private String configPath;
  private volatile ChaosConfig currentConfig;
  private long lastModified = 0;
  private long watcherTimerId = -1;

  @Inject
  Vertx vertx;

  public ChaosManager() {
    // Default constructor for CDI
  }

  @PostConstruct
  public void init() {
    this.configPath = System.getenv().getOrDefault("CHAOS_CONFIG_PATH", DEFAULT_CONFIG_PATH);
    this.currentConfig = new ChaosConfig();
    loadConfig();
    startWatcher();
  }

  public void startWatcher() {
    if (vertx != null) {
      // Check for file modification every 5 seconds
      this.watcherTimerId = vertx.setPeriodic(5000, id -> {
        File file = new File(configPath);
        if (file.exists() && file.lastModified() > lastModified) {
          log.info("🔄 Detect modifications in {}, reloading chaos config...", configPath);
          loadConfig();
        }
      });
    }
  }

  @PreDestroy
  public void destroy() {
    if (watcherTimerId != -1 && vertx != null) {
      vertx.cancelTimer(watcherTimerId);
    }
    halt();
  }

  public synchronized void loadConfig() {
    ChaosResourceSabotage.haltAll();
    File file = new File(configPath);
    if (!file.exists()) {
      log.warn("⚠️ Chaos config file not found at: {}. Using default empty config.", file.getAbsolutePath());
      this.currentConfig = new ChaosConfig();
      return;
    }

    try (InputStream input = new FileInputStream(file)) {
      LoaderOptions loaderOptions = new LoaderOptions();
      Yaml yaml = new Yaml(new Constructor(ChaosConfig.class, loaderOptions));
      ChaosConfig config = yaml.load(input);
      if (config == null) {
        config = new ChaosConfig();
      }
      this.currentConfig = config;
      this.lastModified = file.lastModified();
      log.info("✅ Chaos configuration successfully loaded/reloaded from {}. Total policies: {}", 
          configPath, config.getPolicies().size());

      // Trigger resource sabotage if any policy is enabled
      for (ChaosPolicy policy : config.getPolicies()) {
        if (policy.isEnabled()) {
          if ("cpu".equalsIgnoreCase(policy.getType())) {
            ChaosResourceSabotage.startCpuPressure(policy.getCpuCores(), policy.getDuration());
          } else if ("memory".equalsIgnoreCase(policy.getType())) {
            ChaosResourceSabotage.startMemoryLeak(policy.getMemoryMb(), policy.getDuration());
          }
        }
      }
    } catch (Exception e) {
      log.error("❌ Failed to load/parse chaos configuration from {}", configPath, e);
    }
  }

  public ChaosConfig getConfig() {
    return currentConfig;
  }

  public List<ChaosPolicy> getPolicies() {
    return currentConfig != null ? currentConfig.getPolicies() : new ArrayList<>();
  }

  public ChaosPolicy evaluate(String type, String target) {
    ChaosConfig cfg = currentConfig;
    if (cfg == null || cfg.getPolicies() == null) return null;

    for (ChaosPolicy policy : cfg.getPolicies()) {
      if (policy.isEnabled() && type.equalsIgnoreCase(policy.getType())) {
        if (matches(target, policy.getTarget())) {
          return policy;
        }
      }
    }
    return null;
  }

  private boolean matches(String target, String pattern) {
    if (pattern == null) return false;
    if ("all".equalsIgnoreCase(pattern) || "*".equals(pattern)) return true;
    if (target == null) return false;

    if (pattern.endsWith("*")) {
      String prefix = pattern.substring(0, pattern.length() - 1);
      return target.startsWith(prefix);
    }

    try {
      return target.equals(pattern) || target.matches(pattern);
    } catch (Exception e) {
      return target.contains(pattern);
    }
  }

  public void halt() {
    log.info("🛑 Halt command triggered: disabling all chaos policies.");
    ChaosConfig cfg = currentConfig;
    if (cfg != null && cfg.getPolicies() != null) {
      for (ChaosPolicy policy : cfg.getPolicies()) {
        policy.setEnabled(false);
      }
    }
    ChaosResourceSabotage.haltAll();
  }
}
