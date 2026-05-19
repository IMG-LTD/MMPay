package com.imgltd.mmpay.app.integrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record RelayForwardResponse(
    @JsonProperty("target_id") String targetId,
    @JsonProperty("request_id") String requestId,
    @JsonProperty("byte_count") int byteCount,
    @JsonProperty("payload_sha256") String payloadSha256,
    @JsonProperty("http_status") int httpStatus,
    @JsonProperty("response_truncated") boolean responseTruncated,
    @JsonProperty("error_class") String errorClass,
    boolean synthetic,
    @JsonProperty("dispatched_at") Instant dispatchedAt) {
  static RelayForwardResponse from(LicenseRelayLogRow row) {
    return new RelayForwardResponse(
        row.targetId(),
        row.requestId(),
        row.byteCount(),
        row.payloadSha256(),
        row.httpStatus() == null ? 0 : row.httpStatus(),
        row.responseTruncated(),
        row.errorClass(),
        row.synthetic(),
        row.dispatchedAt());
  }
}
