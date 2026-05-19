package com.imgltd.mmpay.app.integrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record IntegrationResponse(
    String id,
    String kind,
    String name,
    String slug,
    @JsonProperty("target_url_masked") String targetUrlMasked,
    String status,
    @JsonProperty("relay_target_id") String relayTargetId,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt) {
  static IntegrationResponse from(IntegrationRow row) {
    return new IntegrationResponse(
        row.id(),
        row.kind(),
        row.name(),
        row.slug(),
        UrlMasker.mask(row.targetUrl()),
        row.status(),
        row.relayTargetId(),
        row.createdAt(),
        row.updatedAt());
  }
}
