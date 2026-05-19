package com.imgltd.mmpay.iam;

public record ServicePrincipalRecord(
    String userId, String registeredClientId, String clientId, String role, String secretFingerprint) {}
