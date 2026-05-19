package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record RefundResponse(
    String id,
    @JsonProperty("payment_intent_id") String paymentIntentId,
    @JsonProperty("merchant_id") String merchantId,
    @JsonProperty("amount_minor") long amountMinor,
    String currency,
    String status,
    @JsonProperty("requested_at") Instant requestedAt) {
  static RefundResponse from(RefundRow row) {
    return new RefundResponse(
        row.id(),
        row.paymentIntentId(),
        row.merchantId(),
        row.amountMinor(),
        row.currency(),
        row.status(),
        row.requestedAt());
  }
}
