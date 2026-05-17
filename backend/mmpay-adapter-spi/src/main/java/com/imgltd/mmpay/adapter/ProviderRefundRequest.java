package com.imgltd.mmpay.adapter;

public record ProviderRefundRequest(String refundOrderId, long amountMinor, String reason) {
  public ProviderRefundRequest {
    AdapterChecks.requireText(refundOrderId, "refundOrderId");
    AdapterChecks.requirePositiveAmount(amountMinor);
    AdapterChecks.requireText(reason, "reason");
  }
}
