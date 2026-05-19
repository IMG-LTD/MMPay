package com.imgltd.mmpay.merchant;

import java.time.Instant;

record MerchantRow(
    String id,
    String displayName,
    String credentialRef,
    String credentialFingerprint,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
