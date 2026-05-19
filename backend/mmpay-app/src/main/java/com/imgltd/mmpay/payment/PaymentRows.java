package com.imgltd.mmpay.payment;

import java.time.Instant;
import java.time.LocalDate;

record ChannelCredentialRow(
    String id, String merchantId, String providerCode, String credentialRef, String credentialFingerprint) {}

record PaymentIntentRow(
    String id,
    String merchantId,
    String channelId,
    String providerOrderId,
    long amountMinor,
    String currency,
    String orderRef,
    String status,
    Instant createdAt,
    Instant updatedAt,
    long version) {}

record RefundRow(
    String id,
    String paymentIntentId,
    String merchantId,
    long amountMinor,
    String currency,
    String status,
    Instant requestedAt) {}

record ReconciliationRunRow(
    long id,
    LocalDate runDate,
    String providerCode,
    String channelId,
    long ingestCount,
    long matchedCount,
    long unmatchedCount,
    String outcome,
    String ackStatus,
    Instant ackAt,
    String ackActor) {}

record WebhookIntegrationRow(
    String id,
    String displayName,
    String targetUrl,
    String secretRef,
    String secretFingerprint,
    String status,
    Instant createdAt,
    Instant updatedAt) {}

record DeliveryLogRow(
    long id,
    String integrationId,
    String paymentIntentId,
    String eventId,
    int attempt,
    Instant scheduledAt,
    Instant dispatchedAt,
    Integer responseStatus,
    Instant nextRetryAt,
    boolean deadLetter) {}
