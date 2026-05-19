package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantCreateRequest(
    String id, @JsonProperty("display_name") String displayName, @JsonProperty("credential_ref") String credentialRef) {}
