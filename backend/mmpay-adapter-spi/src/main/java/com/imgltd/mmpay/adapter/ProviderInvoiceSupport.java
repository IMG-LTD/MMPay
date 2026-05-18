package com.imgltd.mmpay.adapter;

public record ProviderInvoiceSupport(boolean supported, String reason) {
  public static ProviderInvoiceSupport unsupportedByCurrentProvider() {
    return new ProviderInvoiceSupport(false, "unsupported by current provider");
  }

  public ProviderInvoiceSupport {
    if (!supported) {
      AdapterChecks.requireText(reason, "reason");
    }
  }
}
