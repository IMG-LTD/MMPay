package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentIntentTest {
  @Test
  void newIntentStartsInRequiresPaymentState() {
    PaymentIntent intent =
        PaymentIntent.create("pi_001", 1999, "CNY", "order-001", "idem-001", Instant.EPOCH);

    assertEquals("pi_001", intent.id());
    assertEquals("idem-001", intent.idempotencyKey());
    assertEquals(PaymentIntentStatus.PENDING, intent.status());
  }

  @Test
  void intentCanMoveThroughSubmittedToSucceeded() {
    PaymentIntent intent =
        PaymentIntent.create("pi_002", 2999, "CNY", "order-002", "idem-002", Instant.EPOCH);

    PaymentIntent succeeded = intent.markSubmitted().markSucceeded();

    assertEquals(PaymentIntentStatus.SUCCEEDED, succeeded.status());
    assertEquals(PaymentIntentStatus.PENDING, intent.status());
  }

  @Test
  void pendingIntentCanBeCancelledBeforeProviderSubmission() {
    PaymentIntent intent =
        PaymentIntent.create("pi_cancel", 2999, "CNY", "order-cancel", "idem-cancel", Instant.EPOCH);

    assertEquals(PaymentIntentStatus.CANCELLED, intent.cancelPending().status());
  }

  @Test
  void succeededIntentCanMoveThroughPartialAndFullRefundStates() {
    PaymentIntent succeeded =
        PaymentIntent.create("pi_refund", 2999, "CNY", "order-refund", "idem-refund", Instant.EPOCH)
            .markSubmitted()
            .markSucceeded();

    assertEquals(PaymentIntentStatus.PARTIALLY_REFUNDED, succeeded.markPartiallyRefunded().status());
    assertEquals(PaymentIntentStatus.REFUNDED, succeeded.markRefunded().status());
  }

  @Test
  void intentRejectsNonPositiveAmounts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PaymentIntent.create("pi_bad", 0, "CNY", "order-bad", "idem-bad", Instant.EPOCH));
  }

  @Test
  void intentRejectsBlankIdempotencyKeys() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PaymentIntent.create("pi_bad", 100, "CNY", "order-bad", " ", Instant.EPOCH));
  }
}
