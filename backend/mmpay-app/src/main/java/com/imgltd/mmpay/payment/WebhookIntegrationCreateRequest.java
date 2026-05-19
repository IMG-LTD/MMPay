package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WebhookIntegrationCreateRequest(
    String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("secret_ref") String secretRef,
    @JsonProperty("legacy_header_alias") String legacyHeaderAlias) {}
