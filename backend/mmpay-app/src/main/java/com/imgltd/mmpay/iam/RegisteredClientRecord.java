package com.imgltd.mmpay.iam;

import java.util.Set;

public record RegisteredClientRecord(
    String id, String clientId, String secretFingerprint, Set<String> grants, Set<String> scopes) {}
