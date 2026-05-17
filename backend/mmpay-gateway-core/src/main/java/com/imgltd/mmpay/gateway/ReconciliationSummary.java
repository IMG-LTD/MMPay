package com.imgltd.mmpay.gateway;

import java.util.List;

public record ReconciliationSummary(
    long succeededCount, long failedCount, long succeededAmountMinor) {
  public static ReconciliationSummary fromTransactions(List<Transaction> transactions) {
    long succeededCount = 0L;
    long failedCount = 0L;
    long succeededAmountMinor = 0L;

    for (Transaction transaction : transactions) {
      if (transaction.status() == TransactionStatus.SUCCEEDED) {
        succeededCount++;
        succeededAmountMinor += transaction.amountMinor();
      }
      if (transaction.status() == TransactionStatus.FAILED) {
        failedCount++;
      }
    }

    return new ReconciliationSummary(succeededCount, failedCount, succeededAmountMinor);
  }
}
