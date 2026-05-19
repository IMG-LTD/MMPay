package com.imgltd.mmpay.app.integrations;

import java.time.Instant;

record IntegrationRow(
    String id,
    String kind,
    String name,
    String slug,
    String targetUrl,
    String status,
    String relayTargetId,
    Instant createdAt,
    Instant updatedAt) {}

record RelayTargetRow(String id, String targetUrl, String status) {}

record LicenseRelayLogRow(
    long id,
    String targetId,
    String requestId,
    int attempt,
    int byteCount,
    String payloadSha256,
    Integer httpStatus,
    String responseSha256,
    Integer responseSizeBytes,
    boolean responseTruncated,
    String errorClass,
    boolean synthetic,
    boolean deadLetter,
    Instant dispatchedAt) {}
