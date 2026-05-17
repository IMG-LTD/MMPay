package com.imgltd.mmpay.gateway;

import java.time.Instant;
import java.util.Objects;

public record Transaction(
    String id,
    String paymentIntentId,
    long amountMinor,
    String currency,
    Instant createdAt,
    TransactionStatus status) {
  public static Transaction create(
      String id, String paymentIntentId, long amountMinor, String currency, Instant createdAt) {
    DomainChecks.requirePositiveAmount(amountMinor);
    return new Transaction(
        DomainChecks.requireText(id, "id"),
        DomainChecks.requireText(paymentIntentId, "paymentIntentId"),
        amountMinor,
        DomainChecks.requireText(currency, "currency"),
        Objects.requireNonNull(createdAt, "createdAt"),
        TransactionStatus.PENDING);
  }

  public Transaction markSucceeded() {
    if (status != TransactionStatus.PENDING) {
      throw new IllegalStateException("Expected PENDING but was " + status);
    }
    return new Transaction(
        id, paymentIntentId, amountMinor, currency, createdAt, TransactionStatus.SUCCEEDED);
  }

  public Transaction markFailed() {
    if (status != TransactionStatus.PENDING) {
      throw new IllegalStateException("Expected PENDING but was " + status);
    }
    return new Transaction(
        id, paymentIntentId, amountMinor, currency, createdAt, TransactionStatus.FAILED);
  }
}
