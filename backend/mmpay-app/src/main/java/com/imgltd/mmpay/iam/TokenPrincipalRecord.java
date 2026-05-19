package com.imgltd.mmpay.iam;

import java.time.Instant;

public record TokenPrincipalRecord(String principalName, String role, Instant expiresAt) {}
