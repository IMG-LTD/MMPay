package com.imgltd.mmpay.adapter;

import java.util.Objects;

public record ProviderRefundResult(String providerRefundId, ProviderPaymentStatus status) {
  public ProviderRefundResult {
    AdapterChecks.requireText(providerRefundId, "providerRefundId");
    Objects.requireNonNull(status, "status");
  }
}
