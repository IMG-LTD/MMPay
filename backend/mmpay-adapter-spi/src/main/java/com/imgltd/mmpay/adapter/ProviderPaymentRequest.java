package com.imgltd.mmpay.adapter;

public record ProviderPaymentRequest(
    String merchantOrderId,
    long amountMinor,
    String currency,
    String subject,
    String returnUrl,
    String notifyUrl) {
  public ProviderPaymentRequest {
    AdapterChecks.requireText(merchantOrderId, "merchantOrderId");
    AdapterChecks.requirePositiveAmount(amountMinor);
    AdapterChecks.requireText(currency, "currency");
    AdapterChecks.requireText(subject, "subject");
    AdapterChecks.requireText(returnUrl, "returnUrl");
    AdapterChecks.requireText(notifyUrl, "notifyUrl");
  }
}
