package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record ChannelResponse(
    String id,
    @JsonProperty("merchant_id") String merchantId,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("provider_code") String providerCode,
    @JsonProperty("credential_ref") String credentialRef,
    @JsonProperty("credential_fingerprint") String credentialFingerprint,
    String status,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt) {
  static ChannelResponse from(ChannelRow row) {
    return new ChannelResponse(
        row.id(),
        row.merchantId(),
        row.displayName(),
        row.providerCode(),
        row.credentialRef(),
        row.credentialFingerprint(),
        row.status(),
        row.createdAt(),
        row.updatedAt());
  }
}
