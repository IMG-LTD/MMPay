package com.imgltd.mmpay.huifu;

import com.imgltd.mmpay.gateway.TransactionStatus;

public final class HuifuSettlementStatusMapper {
  public TransactionStatus toTransactionStatus(String providerStatus) {
    return switch (providerStatus) {
      case "SUCCESS" -> TransactionStatus.SUCCEEDED;
      case "FAILED" -> TransactionStatus.FAILED;
      default -> throw new IllegalArgumentException("Unknown Huifu settlement status");
    };
  }
}
