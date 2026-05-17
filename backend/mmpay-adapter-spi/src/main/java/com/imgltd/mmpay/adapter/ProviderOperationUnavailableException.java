package com.imgltd.mmpay.adapter;

public final class ProviderOperationUnavailableException extends RuntimeException {
  private final String providerCode;
  private final String operation;

  public ProviderOperationUnavailableException(String providerCode, String operation, String message) {
    super(message);
    this.providerCode = AdapterChecks.requireText(providerCode, "providerCode");
    this.operation = AdapterChecks.requireText(operation, "operation");
  }

  public String providerCode() {
    return providerCode;
  }

  public String operation() {
    return operation;
  }
}
