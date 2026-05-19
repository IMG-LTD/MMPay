package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentIntentCreateRequest(
    @JsonProperty("channel_id") String channelId,
    @JsonProperty("amount_minor") long amountMinor,
    String currency,
    @JsonProperty("order_ref") String orderRef) {}
