package com.imgltd.mmpay.app.integrations;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntegrationCreateRequest(
    String id,
    String kind,
    String name,
    String slug,
    @JsonProperty("target_url") String targetUrl,
    @JsonProperty("secret_ref") String secretRef) {}
