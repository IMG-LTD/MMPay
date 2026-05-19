package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public record MerchantResponse(
    String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("credential_ref") String credentialRef,
    @JsonProperty("credential_fingerprint") String credentialFingerprint,
    String status,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt) {
  static MerchantResponse from(MerchantRow row) {
    return new MerchantResponse(
        row.id(),
        row.displayName(),
        row.credentialRef(),
        row.credentialFingerprint(),
        row.status(),
        row.createdAt(),
        row.updatedAt());
  }
}
