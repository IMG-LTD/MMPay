package com.imgltd.mmpay.iam;

public record RefreshTokenRecord(String registeredClientId, String principalName, String refreshToken, String role) {}
