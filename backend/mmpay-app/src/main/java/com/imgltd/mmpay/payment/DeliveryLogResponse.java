package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record DeliveryLogResponse(
    long id,
    @JsonProperty("integration_id") String integrationId,
    @JsonProperty("payment_intent_id") String paymentIntentId,
    @JsonProperty("event_id") String eventId,
    int attempt,
    @JsonProperty("scheduled_at") Instant scheduledAt,
    @JsonProperty("dispatched_at") Instant dispatchedAt,
    @JsonProperty("response_status") Integer responseStatus,
    @JsonProperty("next_retry_at") Instant nextRetryAt,
    @JsonProperty("dead_letter") boolean deadLetter) {
  static DeliveryLogResponse from(DeliveryLogRow row) {
    return new DeliveryLogResponse(
        row.id(),
        row.integrationId(),
        row.paymentIntentId(),
        row.eventId(),
        row.attempt(),
        row.scheduledAt(),
        row.dispatchedAt(),
        row.responseStatus(),
        row.nextRetryAt(),
        row.deadLetter());
  }
}
