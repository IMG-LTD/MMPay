package com.imgltd.mmpay.gateway;

import java.util.Objects;
import java.util.Set;

public record ProviderBinding(String providerCode, CredentialBinding credentialBinding) {
  private static final Set<String> RESERVED_CODES = Set.of("none", "mock", "dummy");
  private static final Set<String> SUPPORTED_CODES = Set.of("huifu");

  public ProviderBinding {
    providerCode = DomainChecks.requireText(providerCode, "providerCode");
    credentialBinding = Objects.requireNonNull(credentialBinding, "credentialBinding");
    if (RESERVED_CODES.contains(providerCode)) {
      throw new IllegalArgumentException("providerCode is reserved: " + providerCode);
    }
    if (!SUPPORTED_CODES.contains(providerCode)) {
      throw new IllegalArgumentException("providerCode is unknown: " + providerCode);
    }
  }

  public static ProviderBinding create(
      String providerCode, CredentialBinding credentialBinding) {
    return new ProviderBinding(providerCode, credentialBinding);
  }
}
