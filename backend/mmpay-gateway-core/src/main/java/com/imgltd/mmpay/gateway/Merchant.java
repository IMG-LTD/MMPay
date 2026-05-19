package com.imgltd.mmpay.gateway;

import java.util.Objects;

public record Merchant(
    String id,
    String displayName,
    CredentialBinding credentialBinding,
    MerchantStatus status,
    String tenantId) {
  private static final String DEFAULT_TENANT_ID = "default";

  public Merchant {
    id = DomainChecks.requireText(id, "id");
    displayName = DomainChecks.requireText(displayName, "displayName");
    credentialBinding = Objects.requireNonNull(credentialBinding, "credentialBinding");
    status = Objects.requireNonNull(status, "status");
    tenantId = DomainChecks.requireText(tenantId, "tenantId");
  }

  public static Merchant create(
      String id, String displayName, CredentialBinding credentialBinding) {
    return new Merchant(id, displayName, credentialBinding, MerchantStatus.ACTIVE, DEFAULT_TENANT_ID);
  }

  public String credentialRef() {
    return credentialBinding.credentialRef();
  }

  public String credentialFingerprint() {
    return credentialBinding.credentialFingerprint();
  }
}
