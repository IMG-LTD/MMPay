package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HuifuProviderDescriptorTest {
  @Test
  void exposesHuifuDescriptorForCredentialBindingHints() {
    var descriptor = HuifuProviderDescriptor.create();

    assertEquals("huifu", descriptor.code());
    assertEquals("Huifu", descriptor.displayName());
    assertTrue(descriptor.requiredEnvKeys().contains("HUIFU_MERCHANT_ID"));
    assertTrue(descriptor.requiredEnvKeys().contains("HUIFU_RSA_PRIVATE_KEY"));
    assertTrue(descriptor.requiredEnvKeys().contains("HUIFU_WEBHOOK_ENDPOINT_KEY"));
  }
}
