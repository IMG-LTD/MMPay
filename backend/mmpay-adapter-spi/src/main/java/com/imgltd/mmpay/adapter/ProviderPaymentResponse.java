package com.imgltd.mmpay.adapter;

public record ProviderPaymentResponse(String providerOrderId, String redirectUrl) {
  public ProviderPaymentResponse {
    AdapterChecks.requireText(providerOrderId, "providerOrderId");
    AdapterChecks.requireText(redirectUrl, "redirectUrl");
  }
}
