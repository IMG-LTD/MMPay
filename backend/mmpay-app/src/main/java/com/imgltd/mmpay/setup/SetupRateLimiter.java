package com.imgltd.mmpay.setup;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SetupRateLimiter {
  private static final int MAX_POSTS_PER_WINDOW = 5;
  private static final Duration WINDOW = Duration.ofMinutes(1);
  private final Clock clock;
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  public SetupRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public boolean tryAcquire(String remoteAddress) {
    var key = remoteAddress == null ? "" : remoteAddress;
    var now = Instant.now(clock);
    var bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(now));
    synchronized (bucket) {
      bucket.resetIfExpired(now);
      if (bucket.count >= MAX_POSTS_PER_WINDOW) {
        return false;
      }
      bucket.count++;
      return true;
    }
  }

  private static final class Bucket {
    private Instant windowStart;
    private int count;

    private Bucket(Instant windowStart) {
      this.windowStart = windowStart;
    }

    private void resetIfExpired(Instant now) {
      if (Duration.between(windowStart, now).compareTo(WINDOW) >= 0) {
        windowStart = now;
        count = 0;
      }
    }
  }
}
