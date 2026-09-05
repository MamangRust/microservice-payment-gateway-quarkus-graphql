package com.sanedge.common.chaos;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ChaosConfig {
  private String warmupDuration = "0s";
  private boolean enableDefaultIgnored = true;
  private List<ChaosPolicy> policies = new ArrayList<>();
}
