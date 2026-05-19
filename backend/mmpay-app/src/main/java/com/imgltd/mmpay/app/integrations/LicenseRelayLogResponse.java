package com.imgltd.mmpay.app.integrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record LicenseRelayLogResponse(
    long id,
    @JsonProperty("target_id") String targetId,
    @JsonProperty("request_id") String requestId,
    int attempt,
    @JsonProperty("byte_count") int byteCount,
    @JsonProperty("payload_sha256") String payloadSha256,
    @JsonProperty("http_status") Integer httpStatus,
    @JsonProperty("response_sha256") String responseSha256,
    @JsonProperty("response_size_bytes") Integer responseSizeBytes,
    @JsonProperty("response_truncated") boolean responseTruncated,
    @JsonProperty("error_class") String errorClass,
    boolean synthetic,
    @JsonProperty("dead_letter") boolean deadLetter,
    @JsonProperty("dispatched_at") Instant dispatchedAt) {
  static LicenseRelayLogResponse from(LicenseRelayLogRow row) {
    return new LicenseRelayLogResponse(
        row.id(),
        row.targetId(),
        row.requestId(),
        row.attempt(),
        row.byteCount(),
        row.payloadSha256(),
        row.httpStatus(),
        row.responseSha256(),
        row.responseSizeBytes(),
        row.responseTruncated(),
        row.errorClass(),
        row.synthetic(),
        row.deadLetter(),
        row.dispatchedAt());
  }
}
