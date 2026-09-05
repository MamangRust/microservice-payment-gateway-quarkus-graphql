package com.sanedge.common.chaos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChaosPolicy {
  private String name;
  private String type; // "http", "sql", "cpu", "memory"
  private String target; // "all", "/api/v1/payments", "SELECT", etc.
  @Builder.Default
  private boolean enabled = false;
  @Builder.Default
  private double errorChance = 0.0; // 0.0 to 1.0
  private int errorCode; // e.g., 503, 429
  private String errorMessage; // e.g. "PgException"
  private String errorBody; // e.g. json payload
  private long latencyMs; // delay duration
  private boolean dropConnection;
  private boolean exhaustConnections;
  private int cpuCores;
  private int memoryMb;
  private String duration; // e.g. "5m"
  private boolean dropMessage; // kafka: silently drop
  private boolean rejectMessage; // kafka: fail with error
  private String grpcStatus;     // "UNAVAILABLE", "DEADLINE_EXCEEDED", "INTERNAL", etc.
  private boolean resetStream;   // abruptly close stream
  private long deadlineMs;       // simulate deadline exceeded
}
