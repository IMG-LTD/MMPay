package com.imgltd.mmpay.license;

import java.util.Objects;

public final class LicenseRelay {
  private final LicenseDeliveryTarget deliveryTarget;

  public LicenseRelay(LicenseDeliveryTarget deliveryTarget) {
    this.deliveryTarget = Objects.requireNonNull(deliveryTarget, "deliveryTarget");
  }

  public LicenseRelayReceipt forward(byte[] opaqueLicenseBytes, String tenantId) {
    if (opaqueLicenseBytes == null || opaqueLicenseBytes.length == 0) {
      throw new IllegalArgumentException("opaque license bytes must not be empty");
    }
    if (tenantId == null || tenantId.isBlank()) {
      throw new IllegalArgumentException("tenantId must not be blank");
    }

    byte[] payload = opaqueLicenseBytes.clone();
    deliveryTarget.deliver(tenantId, payload);
    return new LicenseRelayReceipt(tenantId, payload.length);
  }
}
