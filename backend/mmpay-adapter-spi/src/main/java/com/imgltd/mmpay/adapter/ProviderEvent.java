package com.imgltd.mmpay.adapter;

import java.time.Instant;
import java.util.Objects;

public record ProviderEvent(
    String eventId,
    String providerOrderId,
    ProviderPaymentStatus status,
    long amountMinor,
    Instant occurredAt) {
  public ProviderEvent {
    AdapterChecks.requireText(eventId, "eventId");
    AdapterChecks.requireText(providerOrderId, "providerOrderId");
    Objects.requireNonNull(status, "status");
    AdapterChecks.requirePositiveAmount(amountMinor);
    Objects.requireNonNull(occurredAt, "occurredAt");
  }
}
