package com.imgltd.mmpay.iam;

import java.time.Instant;
import java.util.Set;

public record TokenRecord(
    String id,
    String registeredClientId,
    String principalName,
    String grantType,
    Set<String> scopes,
    String accessToken,
    Instant issuedAt,
    Instant expiresAt,
    String refreshToken,
    Instant refreshTokenIssuedAt,
    Instant refreshTokenExpiresAt) {}
