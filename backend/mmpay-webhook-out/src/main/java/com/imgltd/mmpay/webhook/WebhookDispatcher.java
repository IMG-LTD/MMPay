package com.imgltd.mmpay.webhook;

import java.util.Map;
import java.util.Objects;

public final class WebhookDispatcher {
  private static final String EVENT_ID_FIELD = "eventId";

  private final WebhookSigner signer;
  private final WebhookIdempotencyStore idempotencyStore;
  private final WebhookDeliveryTarget target;

  public WebhookDispatcher(
      WebhookSigner signer, WebhookIdempotencyStore idempotencyStore, WebhookDeliveryTarget target) {
    this.signer = Objects.requireNonNull(signer, "signer");
    this.idempotencyStore = Objects.requireNonNull(idempotencyStore, "idempotencyStore");
    this.target = Objects.requireNonNull(target, "target");
  }

  public WebhookDeliveryResult dispatch(Map<String, ?> payload) {
    String eventId = eventId(payload);
    if (!idempotencyStore.reserve(eventId)) {
      return WebhookDeliveryResult.duplicate(eventId);
    }
    try {
      WebhookTargetResponse response = target.deliver(signer.sign(payload));
      if (!response.isAccepted()) {
        throw new IllegalStateException("webhook target rejected event: " + eventId);
      }
      idempotencyStore.markDelivered(eventId);
      return WebhookDeliveryResult.sent(eventId);
    } catch (RuntimeException exception) {
      idempotencyStore.release(eventId);
      throw exception;
    }
  }

  private static String eventId(Map<String, ?> payload) {
    Objects.requireNonNull(payload, "payload");
    Object value = payload.get(EVENT_ID_FIELD);
    if (!(value instanceof String eventId) || eventId.isBlank()) {
      throw new IllegalArgumentException("payload.eventId must not be blank");
    }
    return eventId;
  }
}
