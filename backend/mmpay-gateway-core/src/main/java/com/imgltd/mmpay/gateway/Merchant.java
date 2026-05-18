package com.imgltd.mmpay.gateway;

public record Merchant(String id, String displayName, String credentialHandle) {
  public static Merchant create(String id, String displayName, String credentialHandle) {
    return new Merchant(
        DomainChecks.requireText(id, "id"),
        DomainChecks.requireText(displayName, "displayName"),
        DomainChecks.requireText(credentialHandle, "credentialHandle"));
  }
}
