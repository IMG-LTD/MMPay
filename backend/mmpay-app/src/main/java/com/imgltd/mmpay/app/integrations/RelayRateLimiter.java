package com.imgltd.mmpay.app.integrations;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class RelayRateLimiter {
  private final int maxRequests;
  private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

  RelayRateLimiter(int maxRequests) {
    this.maxRequests = maxRequests;
  }

  boolean allow(String certificateKey) {
    if (maxRequests <= 0) {
      return true;
    }
    int count = counters.computeIfAbsent(certificateKey, ignored -> new AtomicInteger()).incrementAndGet();
    return count <= maxRequests;
  }
}
