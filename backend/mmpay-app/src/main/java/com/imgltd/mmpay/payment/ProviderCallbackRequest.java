package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ProviderCallbackRequest(
    @JsonProperty("provider_event_id") String providerEventId,
    @JsonProperty("payment_intent_id") String paymentIntentId,
    String status,
    @JsonProperty("amount_minor") long amountMinor,
    @JsonProperty("occurred_at") Instant occurredAt) {}
