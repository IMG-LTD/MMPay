package com.imgltd.mmpay.gateway;

import java.time.Instant;
import java.util.Objects;

public record PaymentIntent(
    String id,
    long amountMinor,
    String currency,
    String orderRef,
    Instant createdAt,
    PaymentIntentStatus status) {
  public static PaymentIntent create(
      String id, long amountMinor, String currency, String orderRef, Instant createdAt) {
    DomainChecks.requirePositiveAmount(amountMinor);
    return new PaymentIntent(
        DomainChecks.requireText(id, "id"),
        amountMinor,
        DomainChecks.requireText(currency, "currency"),
        DomainChecks.requireText(orderRef, "orderRef"),
        Objects.requireNonNull(createdAt, "createdAt"),
        PaymentIntentStatus.REQUIRES_PAYMENT);
  }

  public PaymentIntent markProcessing() {
    requireStatus(PaymentIntentStatus.REQUIRES_PAYMENT);
    return withStatus(PaymentIntentStatus.PROCESSING);
  }

  public PaymentIntent markSucceeded() {
    requireStatus(PaymentIntentStatus.PROCESSING);
    return withStatus(PaymentIntentStatus.SUCCEEDED);
  }

  private PaymentIntent withStatus(PaymentIntentStatus nextStatus) {
    return new PaymentIntent(id, amountMinor, currency, orderRef, createdAt, nextStatus);
  }

  private void requireStatus(PaymentIntentStatus expectedStatus) {
    if (status != expectedStatus) {
      throw new IllegalStateException("Expected " + expectedStatus + " but was " + status);
    }
  }
}
