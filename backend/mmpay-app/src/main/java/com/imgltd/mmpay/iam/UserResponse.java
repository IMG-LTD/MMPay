package com.imgltd.mmpay.iam;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserResponse(
    String username,
    String kind,
    String role,
    @JsonProperty("created_at") String createdAt) {}
