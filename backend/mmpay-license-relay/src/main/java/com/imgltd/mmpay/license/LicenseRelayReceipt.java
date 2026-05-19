package com.imgltd.mmpay.license;

public record LicenseRelayReceipt(
    String targetId,
    String requestId,
    int byteCount,
    String payloadSha256,
    int httpStatus,
    String responseSha256,
    int responseSizeBytes,
    boolean responseTruncated,
    String errorClass,
    boolean success) {}
