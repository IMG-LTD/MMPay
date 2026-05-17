package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReconciliationTest {
  @Test
  void summarizesSucceededAndFailedTransactions() {
    Transaction succeeded =
        Transaction.create("txn_ok", "pi_ok", 1000, "CNY", Instant.EPOCH).markSucceeded();
    Transaction failed =
        Transaction.create("txn_fail", "pi_fail", 2000, "CNY", Instant.EPOCH).markFailed();

    ReconciliationSummary summary = ReconciliationSummary.fromTransactions(List.of(succeeded, failed));

    assertEquals(1, summary.succeededCount());
    assertEquals(1, summary.failedCount());
    assertEquals(1000, summary.succeededAmountMinor());
  }
}
