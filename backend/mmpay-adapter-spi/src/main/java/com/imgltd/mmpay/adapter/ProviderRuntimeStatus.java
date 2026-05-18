package com.imgltd.mmpay.adapter;

public record ProviderRuntimeStatus(boolean available, String reason) {
  public static ProviderRuntimeStatus unavailable(String reason) {
    return new ProviderRuntimeStatus(false, reason);
  }

  public ProviderRuntimeStatus {
    if (!available) {
      AdapterChecks.requireText(reason, "reason");
    }
  }
}
