package com.imgltd.mmpay.gateway;

import java.time.Instant;
import java.util.Objects;

public record PaymentIntent(
    String id,
    long amountMinor,
    String currency,
    String orderRef,
    String idempotencyKey,
    Instant createdAt,
    PaymentIntentStatus status) {
  public static PaymentIntent create(
      String id,
      long amountMinor,
      String currency,
      String orderRef,
      String idempotencyKey,
      Instant createdAt) {
    DomainChecks.requirePositiveAmount(amountMinor);
    return new PaymentIntent(
        DomainChecks.requireText(id, "id"),
        amountMinor,
        DomainChecks.requireText(currency, "currency"),
        DomainChecks.requireText(orderRef, "orderRef"),
        DomainChecks.requireText(idempotencyKey, "idempotencyKey"),
        Objects.requireNonNull(createdAt, "createdAt"),
        PaymentIntentStatus.PENDING);
  }

  public PaymentIntent markSubmitted() {
    requireStatus(PaymentIntentStatus.PENDING);
    return withStatus(PaymentIntentStatus.SUBMITTED);
  }

  public PaymentIntent markSucceeded() {
    requireStatus(PaymentIntentStatus.SUBMITTED);
    return withStatus(PaymentIntentStatus.SUCCEEDED);
  }

  public PaymentIntent cancelPending() {
    requireStatus(PaymentIntentStatus.PENDING);
    return withStatus(PaymentIntentStatus.CANCELLED);
  }

  public PaymentIntent markPartiallyRefunded() {
    requireRefundable();
    return withStatus(PaymentIntentStatus.PARTIALLY_REFUNDED);
  }

  public PaymentIntent markRefunded() {
    requireRefundable();
    return withStatus(PaymentIntentStatus.REFUNDED);
  }

  private PaymentIntent withStatus(PaymentIntentStatus nextStatus) {
    return new PaymentIntent(
        id, amountMinor, currency, orderRef, idempotencyKey, createdAt, nextStatus);
  }

  private void requireStatus(PaymentIntentStatus expectedStatus) {
    if (status != expectedStatus) {
      throw new IllegalStateException("Expected " + expectedStatus + " but was " + status);
    }
  }

  private void requireRefundable() {
    if (status != PaymentIntentStatus.SUCCEEDED && status != PaymentIntentStatus.PARTIALLY_REFUNDED) {
      throw new IllegalStateException("Expected refundable intent but was " + status);
    }
  }
}
