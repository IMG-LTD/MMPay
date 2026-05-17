package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PaymentIntentTest {
  @Test
  void newIntentStartsInRequiresPaymentState() {
    PaymentIntent intent = PaymentIntent.create("pi_001", 1999, "CNY", "order-001", Instant.EPOCH);

    assertEquals("pi_001", intent.id());
    assertEquals(PaymentIntentStatus.REQUIRES_PAYMENT, intent.status());
  }

  @Test
  void intentCanMoveThroughProcessingToSucceeded() {
    PaymentIntent intent = PaymentIntent.create("pi_002", 2999, "CNY", "order-002", Instant.EPOCH);

    PaymentIntent succeeded = intent.markProcessing().markSucceeded();

    assertEquals(PaymentIntentStatus.SUCCEEDED, succeeded.status());
    assertEquals(PaymentIntentStatus.REQUIRES_PAYMENT, intent.status());
  }

  @Test
  void intentRejectsNonPositiveAmounts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PaymentIntent.create("pi_bad", 0, "CNY", "order-bad", Instant.EPOCH));
  }
}
