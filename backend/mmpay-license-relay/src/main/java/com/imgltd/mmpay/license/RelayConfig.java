package com.imgltd.mmpay.license;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public record RelayConfig(Map<String, URI> targets, int responseCapBytes, Duration timeout) {
  private static final int MIN_RESPONSE_CAP_BYTES = 1;

  public RelayConfig(Map<String, URI> targets, int responseCapBytes) {
    this(targets, responseCapBytes, Duration.ofSeconds(5));
  }

  public RelayConfig {
    Objects.requireNonNull(targets, "targets");
    Objects.requireNonNull(timeout, "timeout");
    targets = Map.copyOf(targets);
    if (responseCapBytes < MIN_RESPONSE_CAP_BYTES) {
      throw new IllegalArgumentException("responseCapBytes must be positive");
    }
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
  }

  URI targetUri(String targetId) {
    URI targetUri = targets.get(targetId);
    if (targetUri == null) {
      throw new IllegalArgumentException("relay target not found");
    }
    return targetUri;
  }
}
