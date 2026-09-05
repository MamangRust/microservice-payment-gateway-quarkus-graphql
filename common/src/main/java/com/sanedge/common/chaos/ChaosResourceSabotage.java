package com.sanedge.common.chaos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChaosResourceSabotage {
  private static final Logger log = LoggerFactory.getLogger(ChaosResourceSabotage.class);

  private static final List<Thread> cpuThreads = new ArrayList<>();
  private static volatile boolean cpuSpinning = false;

  private static final List<byte[]> leakedMemory = new ArrayList<>();
  private static volatile Timer memoryTimer = null;
  private static volatile Timer rollbackTimer = null;

  public static synchronized void startCpuPressure(int cores, String durationStr) {
    if (cpuSpinning) return;
    cpuSpinning = true;
    int numCores = cores > 0 ? cores : Runtime.getRuntime().availableProcessors();
    log.info("⚙️ Starting CPU pressure on {} threads...", numCores);
    
    for (int i = 0; i < numCores; i++) {
      final int workerId = i;
      Thread t = new Thread(() -> {
        while (cpuSpinning) {
          blackhole(Math.sqrt(Math.random()));
        }
      });
      t.setDaemon(true);
      t.setName("chaos-cpu-worker-" + workerId);
      t.start();
      cpuThreads.add(t);
    }

    long durationMs = parseDuration(durationStr);
    if (durationMs > 0) {
      registerAmnesiaCleanup(durationMs);
    }
  }

  public static synchronized void startMemoryLeak(int targetMb, String durationStr) {
    if (memoryTimer != null) return;
    int maxMb = targetMb > 0 ? targetMb : 100;
    log.info("💾 Starting memory leak simulation. Target: {} MB", maxMb);
    
    memoryTimer = new Timer("chaos-memory-leak", true);
    memoryTimer.scheduleAtFixedRate(new TimerTask() {
      private int allocated = 0;

      @Override
      public void run() {
        if (!cpuSpinning && memoryTimer == null) {
          cancel();
          return;
        }
        if (allocated >= maxMb) {
          cancel();
          log.info("💾 Memory leak target of {} MB reached.", maxMb);
          return;
        }
        // Allocate chunks of 10MB
        int chunkSize = Math.min(10, maxMb - allocated);
        try {
          leakedMemory.add(new byte[chunkSize * 1024 * 1024]);
          allocated += chunkSize;
          log.info("💾 Leaked {} MB total in heap memory.", allocated);
        } catch (OutOfMemoryError oom) {
          log.error("💥 Out of Memory during chaos simulation! Auto-halting.", oom);
          haltAll();
          cancel();
        }
      }
    }, 0, 1000);

    long durationMs = parseDuration(durationStr);
    if (durationMs > 0) {
      registerAmnesiaCleanup(durationMs);
    }
  }

  public static synchronized void haltAll() {
    log.info("🛑 Halting resource sabotage (CPU pressure & Memory leaks).");
    cpuSpinning = false;
    cpuThreads.clear();
    
    if (memoryTimer != null) {
      memoryTimer.cancel();
      memoryTimer = null;
    }
    if (rollbackTimer != null) {
      rollbackTimer.cancel();
      rollbackTimer = null;
    }
    
    leakedMemory.clear();
    System.gc(); // Suggest Garbage Collection to reclaim memory
  }

  private static long parseDuration(String durationStr) {
    if (durationStr == null || durationStr.isEmpty()) return 0;
    try {
      String val = durationStr.replaceAll("[^0-9]", "");
      int num = Integer.parseInt(val);
      if (durationStr.endsWith("s")) return num * 1000L;
      if (durationStr.endsWith("m")) return num * 60 * 1000L;
      if (durationStr.endsWith("h")) return num * 3600 * 1000L;
      return num;
    } catch (Exception e) {
      return 0;
    }
  }

  private static synchronized void registerAmnesiaCleanup(long durationMs) {
    if (rollbackTimer != null) {
      rollbackTimer.cancel();
    }
    rollbackTimer = new Timer("chaos-amnesia-rollback", true);
    rollbackTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        log.info("⏰ Amnesia Protocol: Rolling back resource pressure after duration elapsed.");
        haltAll();
      }
    }, durationMs);
  }

  private static void blackhole(double val) {
    if (val < 0) {
      log.trace("Blackhole output: {}", val);
    }
  }
}
