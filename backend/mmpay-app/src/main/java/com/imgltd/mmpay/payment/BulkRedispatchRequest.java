package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BulkRedispatchRequest(
    @JsonProperty("integration_id") String integrationId,
    @JsonProperty("event_count") int eventCount,
    Integer rps) {}
