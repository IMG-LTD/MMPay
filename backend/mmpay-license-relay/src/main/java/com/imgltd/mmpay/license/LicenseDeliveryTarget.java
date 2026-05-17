package com.imgltd.mmpay.license;

public interface LicenseDeliveryTarget {
  void deliver(String tenantId, byte[] opaqueLicenseBytes);
}
