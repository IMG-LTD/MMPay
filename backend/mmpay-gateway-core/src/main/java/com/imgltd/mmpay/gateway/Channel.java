package com.imgltd.mmpay.gateway;

import java.util.Objects;

public record Channel(
    String id,
    String merchantId,
    ProviderBinding providerBinding,
    ChannelStatus status,
    String tenantId) {
  private static final String DEFAULT_TENANT_ID = "default";

  public Channel {
    id = DomainChecks.requireText(id, "id");
    merchantId = DomainChecks.requireText(merchantId, "merchantId");
    providerBinding = Objects.requireNonNull(providerBinding, "providerBinding");
    status = Objects.requireNonNull(status, "status");
    tenantId = DomainChecks.requireText(tenantId, "tenantId");
  }

  public static Channel create(String id, String merchantId, ProviderBinding providerBinding) {
    return new Channel(id, merchantId, providerBinding, ChannelStatus.ACTIVE, DEFAULT_TENANT_ID);
  }

  public String providerCode() {
    return providerBinding.providerCode();
  }

  public String credentialRef() {
    return providerBinding.credentialBinding().credentialRef();
  }

  public String credentialFingerprint() {
    return providerBinding.credentialBinding().credentialFingerprint();
  }
}
