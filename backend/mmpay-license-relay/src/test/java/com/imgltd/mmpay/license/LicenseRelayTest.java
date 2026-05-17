package com.imgltd.mmpay.license;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LicenseRelayTest {
  @Test
  void forwardsOpaqueVendorSignedLicenseBytesWithoutParsingClaims() {
    CapturingDeliveryTarget target = new CapturingDeliveryTarget();
    LicenseRelay relay = new LicenseRelay(target);
    byte[] signedLicense = "opaque-vendor-signed-license-bytes".getBytes();

    LicenseRelayReceipt receipt = relay.forward(signedLicense, "tenant-001");

    assertEquals("tenant-001", receipt.tenantId());
    assertArrayEquals(signedLicense, target.lastPayload());
  }

  private static final class CapturingDeliveryTarget implements LicenseDeliveryTarget {
    private byte[] lastPayload;

    @Override
    public void deliver(String tenantId, byte[] opaqueLicenseBytes) {
      lastPayload = opaqueLicenseBytes.clone();
    }

    byte[] lastPayload() {
      return lastPayload.clone();
    }
  }
}
