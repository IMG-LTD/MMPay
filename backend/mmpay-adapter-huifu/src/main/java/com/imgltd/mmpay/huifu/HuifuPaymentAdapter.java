package com.imgltd.mmpay.huifu;

import com.imgltd.mmpay.adapter.PaymentProviderAdapter;
import com.imgltd.mmpay.adapter.ProviderCapability;
import com.imgltd.mmpay.adapter.ProviderEvent;
import com.imgltd.mmpay.adapter.ProviderInvoiceSupport;
import com.imgltd.mmpay.adapter.ProviderOperationUnavailableException;
import com.imgltd.mmpay.adapter.ProviderPaymentRequest;
import com.imgltd.mmpay.adapter.ProviderPaymentResponse;
import com.imgltd.mmpay.adapter.ProviderPaymentStatus;
import com.imgltd.mmpay.adapter.ProviderRefundRequest;
import com.imgltd.mmpay.adapter.ProviderRefundResult;
import com.imgltd.mmpay.adapter.ProviderRuntimeStatus;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HuifuPaymentAdapter implements PaymentProviderAdapter {
  private static final String PROVIDER_CODE = "huifu";
  private static final Set<ProviderCapability> CAPABILITIES =
      Set.copyOf(EnumSet.allOf(ProviderCapability.class));

  private final HuifuCredentialHandles credentials;
  private final HuifuInboundNotifyVerifier inboundVerifier;

  public HuifuPaymentAdapter(HuifuCredentialHandles credentials) {
    this(credentials, null);
  }

  public HuifuPaymentAdapter(
      HuifuCredentialHandles credentials, HuifuInboundNotifyVerifier inboundVerifier) {
    this.credentials = Objects.requireNonNull(credentials, "credentials");
    this.inboundVerifier = inboundVerifier;
  }

  @Override
  public String providerCode() {
    return PROVIDER_CODE;
  }

  @Override
  public Set<ProviderCapability> capabilities() {
    return CAPABILITIES;
  }

  @Override
  public ProviderRuntimeStatus runtimeStatus() {
    return ProviderRuntimeStatus.unavailable("live provider client is not wired");
  }

  @Override
  public ProviderInvoiceSupport invoiceSupport() {
    return ProviderInvoiceSupport.unsupportedByCurrentProvider();
  }

  @Override
  public ProviderPaymentResponse createPayment(ProviderPaymentRequest request) {
    Objects.requireNonNull(request, "request");
    throw unavailable("createPayment");
  }

  @Override
  public ProviderPaymentStatus queryPayment(String providerOrderId) {
    requireText(providerOrderId, "providerOrderId");
    throw unavailable("queryPayment");
  }

  @Override
  public ProviderRefundResult refund(String providerOrderId, ProviderRefundRequest request) {
    requireText(providerOrderId, "providerOrderId");
    Objects.requireNonNull(request, "request");
    throw unavailable("refund");
  }

  @Override
  public ProviderEvent verifyInboundWebhook(Map<String, String> headers, byte[] rawBody) {
    Objects.requireNonNull(headers, "headers");
    Objects.requireNonNull(rawBody, "rawBody");
    if (inboundVerifier == null) {
      throw unavailable("verifyInboundWebhook");
    }
    return inboundVerifier.verify(rawBody);
  }

  public HuifuCredentialHandles credentials() {
    return credentials;
  }

  private static ProviderOperationUnavailableException unavailable(String operation) {
    return new ProviderOperationUnavailableException(
        PROVIDER_CODE, operation, "Huifu " + operation + " is not wired to the live provider client");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
