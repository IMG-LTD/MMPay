package com.imgltd.mmpay.gateway;

import java.time.Instant;
import java.util.Objects;

public record Refund(
    String id,
    String transactionId,
    long amountMinor,
    Instant requestedAt,
    RefundStatus status) {
  public static Refund request(
      String id, Transaction transaction, long amountMinor, Instant requestedAt) {
    Objects.requireNonNull(transaction, "transaction");
    requireRefundable(transaction, amountMinor);
    return new Refund(
        DomainChecks.requireText(id, "id"),
        transaction.id(),
        amountMinor,
        Objects.requireNonNull(requestedAt, "requestedAt"),
        RefundStatus.PENDING);
  }

  private static void requireRefundable(Transaction transaction, long amountMinor) {
    if (transaction.status() != TransactionStatus.SUCCEEDED) {
      throw new IllegalStateException("Refund requires a succeeded transaction");
    }
    DomainChecks.requirePositiveAmount(amountMinor);
    if (amountMinor > transaction.amountMinor()) {
      throw new IllegalArgumentException("refund amount must be within transaction amount");
    }
  }
}
