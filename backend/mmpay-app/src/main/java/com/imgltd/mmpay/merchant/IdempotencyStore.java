package com.imgltd.mmpay.merchant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;

public final class IdempotencyStore {
  private static final Duration TTL = Duration.ofHours(24);
  private final Clock clock;
  private final Map<String, IdempotencyEntry> entries = new ConcurrentHashMap<>();

  public IdempotencyStore(Clock clock) {
    this.clock = clock;
  }

  public IdempotencyEntry get(String key, String requestHash) {
    if (key == null || key.isBlank()) {
      return null;
    }
    var entry = entries.get(key);
    if (entry == null || entry.expiresAt().isBefore(Instant.now(clock))) {
      entries.remove(key);
      return null;
    }
    if (!entry.requestHash().equals(requestHash)) {
      throw AdminProblems.conflict(AdminProblems.IDEMPOTENCY_MISMATCH, "idempotency replay mismatch");
    }
    return entry;
  }

  public void put(String key, String requestHash, HttpStatus status, Object body) {
    if (key == null || key.isBlank()) {
      return;
    }
    entries.put(key, new IdempotencyEntry(requestHash, status, body, Instant.now(clock).plus(TTL)));
  }
}
