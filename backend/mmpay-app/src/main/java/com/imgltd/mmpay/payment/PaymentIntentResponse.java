package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record PaymentIntentResponse(
    String id,
    @JsonProperty("merchant_id") String merchantId,
    @JsonProperty("channel_id") String channelId,
    @JsonProperty("provider_order_id") String providerOrderId,
    @JsonProperty("amount_minor") long amountMinor,
    String currency,
    @JsonProperty("order_ref") String orderRef,
    String status,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt) {
  static PaymentIntentResponse from(PaymentIntentRow row) {
    return new PaymentIntentResponse(
        row.id(),
        row.merchantId(),
        row.channelId(),
        row.providerOrderId(),
        row.amountMinor(),
        row.currency(),
        row.orderRef(),
        row.status(),
        row.createdAt(),
        row.updatedAt());
  }
}
