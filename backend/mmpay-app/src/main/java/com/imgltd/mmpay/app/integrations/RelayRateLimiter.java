package com.imgltd.mmpay.app.integrations;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token-bucket rate limiter for the license-relay inbound endpoint. Spec P4 §1.1.2 calls for
 * per-cert-fingerprint AND per-IP buckets. This class implements the per-cert side; the
 * controller layer wires a second instance for per-IP enforcement.
 *
 * <p>Default refill rate: 30 RPS / 60 burst per certificate (spec defaults). The cumulative
 * AtomicInteger model used before P5 wave 6 was wrong: it counted forever and 429-ed permanently
 * after the first burst, with no decay.
 */
final class RelayRateLimiter {
  private final long capacity;
  private final long refillTokensPerSecond;
  private final Clock clock;
  private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

  RelayRateLimiter(int maxRequests) {
    this(maxRequests, Math.max(1, maxRequests / 2), Clock.systemUTC());
  }

  RelayRateLimiter(long capacity, long refillTokensPerSecond, Clock clock) {
    this.capacity = capacity;
    this.refillTokensPerSecond = refillTokensPerSecond;
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  boolean allow(String key) {
    if (capacity <= 0) {
      return true;
    }
    var bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(capacity, clock.millis()));
    return bucket.tryConsume(1, capacity, refillTokensPerSecond, clock.millis());
  }

  private static final class Bucket {
    private double tokens;
    private long lastRefillMillis;

    Bucket(long initialTokens, long now) {
      this.tokens = initialTokens;
      this.lastRefillMillis = now;
    }

    synchronized boolean tryConsume(long requested, long capacity, long refillPerSecond, long nowMillis) {
      refill(capacity, refillPerSecond, nowMillis);
      if (tokens < requested) {
        return false;
      }
      tokens -= requested;
      return true;
    }

    private void refill(long capacity, long refillPerSecond, long nowMillis) {
      var elapsed = nowMillis - lastRefillMillis;
      if (elapsed <= 0) {
        return;
      }
      lastRefillMillis = nowMillis;
      tokens = Math.min((double) capacity, tokens + (elapsed / 1000.0) * refillPerSecond);
    }
  }

  // Used only by tests.
  long bucketCount() {
    return buckets.size();
  }

  Duration timeToFullBucket() {
    return Duration.ofMillis((capacity * 1000L) / Math.max(1L, refillTokensPerSecond));
  }
}
