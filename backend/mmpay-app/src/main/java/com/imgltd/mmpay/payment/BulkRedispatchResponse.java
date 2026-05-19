package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BulkRedispatchResponse(
    @JsonProperty("integration_id") String integrationId,
    @JsonProperty("event_count") int eventCount,
    int rps,
    @JsonProperty("estimated_drain_seconds") int estimatedDrainSeconds) {}
