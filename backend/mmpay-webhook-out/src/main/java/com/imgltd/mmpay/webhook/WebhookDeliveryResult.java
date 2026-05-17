package com.imgltd.mmpay.webhook;

public record WebhookDeliveryResult(WebhookDeliveryStatus status, String eventId) {
  public WebhookDeliveryResult {
    if (eventId == null || eventId.isBlank()) {
      throw new IllegalArgumentException("eventId must not be blank");
    }
  }

  public static WebhookDeliveryResult duplicate(String eventId) {
    return new WebhookDeliveryResult(WebhookDeliveryStatus.DUPLICATE, eventId);
  }

  public static WebhookDeliveryResult sent(String eventId) {
    return new WebhookDeliveryResult(WebhookDeliveryStatus.SENT, eventId);
  }
}
