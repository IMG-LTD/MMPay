package com.imgltd.mmpay.adapter;

public record ProviderPaymentResponse(String providerOrderId, String redirectUrl, String qrCodeUrl) {
  public ProviderPaymentResponse {
    AdapterChecks.requireText(providerOrderId, "providerOrderId");
    if (redirectUrl == null && qrCodeUrl == null) {
      throw new IllegalArgumentException("at least one of redirectUrl or qrCodeUrl must be provided");
    }
  }

  public static ProviderPaymentResponse withRedirect(String providerOrderId, String redirectUrl) {
    return new ProviderPaymentResponse(providerOrderId, redirectUrl, null);
  }

  public static ProviderPaymentResponse withQrCode(String providerOrderId, String qrCodeUrl) {
    return new ProviderPaymentResponse(providerOrderId, null, qrCodeUrl);
  }
}
