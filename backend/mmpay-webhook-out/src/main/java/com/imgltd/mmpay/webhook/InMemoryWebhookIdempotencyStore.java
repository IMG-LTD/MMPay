package com.imgltd.mmpay.webhook;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryWebhookIdempotencyStore implements WebhookIdempotencyStore {
  private final Set<String> reservedEventIds = ConcurrentHashMap.newKeySet();
  private final Set<String> deliveredEventIds = ConcurrentHashMap.newKeySet();

  @Override
  public boolean reserve(String eventId) {
    requireEventId(eventId);
    return !deliveredEventIds.contains(eventId) && reservedEventIds.add(eventId);
  }

  @Override
  public void markDelivered(String eventId) {
    requireEventId(eventId);
    deliveredEventIds.add(eventId);
    reservedEventIds.remove(eventId);
  }

  @Override
  public void release(String eventId) {
    requireEventId(eventId);
    reservedEventIds.remove(eventId);
  }

  private static void requireEventId(String eventId) {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId must not be blank");
    }
  }
}
