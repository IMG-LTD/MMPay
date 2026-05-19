package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefundTest {
  @Test
  void refundCanOnlyBeRequestedForSucceededTransactions() {
    Transaction transaction =
        Transaction.create("txn_001", "pi_001", 1999, "CNY", Instant.EPOCH).markSucceeded();

    Refund refund = Refund.request("rf_001", transaction, 500, Instant.EPOCH);

    assertEquals(RefundStatus.PENDING, refund.status());
    assertEquals("txn_001", refund.transactionId());
  }

  @Test
  void refundRejectsPendingTransactions() {
    Transaction transaction = Transaction.create("txn_002", "pi_002", 1999, "CNY", Instant.EPOCH);

    assertThrows(
        IllegalStateException.class,
        () -> Refund.request("rf_002", transaction, 500, Instant.EPOCH));
  }
}
