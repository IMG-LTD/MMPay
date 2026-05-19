package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record WebhookIntegrationResponse(
    String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("secret_ref") String secretRef,
    @JsonProperty("secret_fingerprint") String secretFingerprint,
    String status,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt) {
  static WebhookIntegrationResponse from(WebhookIntegrationRow row) {
    return new WebhookIntegrationResponse(
        row.id(),
        row.displayName(),
        row.targetUrl(),
        row.secretRef(),
        row.secretFingerprint(),
        row.status(),
        row.createdAt(),
        row.updatedAt());
  }
}
