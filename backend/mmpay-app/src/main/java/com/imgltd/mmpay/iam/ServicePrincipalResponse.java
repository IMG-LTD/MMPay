package com.imgltd.mmpay.iam;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ServicePrincipalResponse(
    @JsonProperty("client_id") String clientId, @JsonProperty("client_secret") String clientSecret) {}
