package com.imgltd.mmpay.adapter;

import java.util.Map;
import java.util.Set;

public interface PaymentProviderAdapter {
  String providerCode();

  Set<ProviderCapability> capabilities();

  ProviderRuntimeStatus runtimeStatus();

  ProviderInvoiceSupport invoiceSupport();

  ProviderPaymentResponse createPayment(ProviderPaymentRequest request);

  ProviderPaymentStatus queryPayment(String providerOrderId);

  ProviderRefundResult refund(String providerOrderId, ProviderRefundRequest request);

  ProviderEvent verifyInboundWebhook(Map<String, String> headers, byte[] rawBody);
}
