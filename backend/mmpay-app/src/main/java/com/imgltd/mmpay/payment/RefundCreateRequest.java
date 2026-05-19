package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RefundCreateRequest(
    @JsonProperty("payment_intent_id") String paymentIntentId,
    @JsonProperty("amount_minor") long amountMinor) {}
