package com.imgltd.mmpay.merchant;

import java.time.Instant;

record ChannelRow(
    String id,
    String merchantId,
    String displayName,
    String providerCode,
    String credentialRef,
    String credentialFingerprint,
    String status,
    Instant createdAt,
    Instant updatedAt) {}
