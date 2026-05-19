package com.imgltd.mmpay.merchant;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MerchantPatchRequest(
    @JsonProperty("display_name") String displayName,
    String status,
    @JsonProperty("credential_ref") @JsonAlias("credentialRef") CredentialRefPatchRequest credentialRef) {}
