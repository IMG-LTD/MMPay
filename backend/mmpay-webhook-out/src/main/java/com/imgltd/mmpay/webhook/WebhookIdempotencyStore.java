package com.imgltd.mmpay.webhook;

public interface WebhookIdempotencyStore {
  boolean reserve(String eventId);

  void markDelivered(String eventId);

  void release(String eventId);
}
