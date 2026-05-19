package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VerifyBindingResponse(
    String status,
    @JsonProperty("stored_fingerprint") String storedFingerprint,
    @JsonProperty("resolved_fingerprint") String resolvedFingerprint) {}
