package com.imgltd.mmpay.gateway;

public record Channel(
    String id, String merchantId, String providerCode, String credentialHandle) {
  public static Channel create(
      String id, String merchantId, String providerCode, String credentialHandle) {
    return new Channel(
        DomainChecks.requireText(id, "id"),
        DomainChecks.requireText(merchantId, "merchantId"),
        DomainChecks.requireText(providerCode, "providerCode"),
        DomainChecks.requireText(credentialHandle, "credentialHandle"));
  }
}
