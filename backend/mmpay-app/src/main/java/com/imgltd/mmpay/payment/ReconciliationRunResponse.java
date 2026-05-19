package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;

public record ReconciliationRunResponse(
    long id,
    @JsonProperty("run_date") LocalDate runDate,
    @JsonProperty("provider_code") String providerCode,
    @JsonProperty("channel_id") String channelId,
    @JsonProperty("ingest_count") long ingestCount,
    @JsonProperty("matched_count") long matchedCount,
    @JsonProperty("unmatched_count") long unmatchedCount,
    String outcome,
    @JsonProperty("ack_status") String ackStatus,
    @JsonProperty("ack_at") Instant ackAt,
    @JsonProperty("ack_actor") String ackActor) {
  static ReconciliationRunResponse from(ReconciliationRunRow row) {
    return new ReconciliationRunResponse(
        row.id(),
        row.runDate(),
        row.providerCode(),
        row.channelId(),
        row.ingestCount(),
        row.matchedCount(),
        row.unmatchedCount(),
        row.outcome(),
        row.ackStatus(),
        row.ackAt(),
        row.ackActor());
  }
}
