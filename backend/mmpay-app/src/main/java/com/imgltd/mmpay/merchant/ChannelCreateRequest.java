package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChannelCreateRequest(
    String id,
    @JsonProperty("display_name") String displayName,
    @JsonProperty("provider_code") String providerCode,
    @JsonProperty("credential_ref") String credentialRef) {}
