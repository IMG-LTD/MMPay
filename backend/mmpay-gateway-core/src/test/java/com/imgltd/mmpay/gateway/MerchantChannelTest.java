package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MerchantChannelTest {
  @Test
  void merchantStoresCredentialReferenceBinding() {
    var binding = CredentialBinding.create("env://ACME_MERCHANT_KEY", "a1b2c3d4");
    Merchant merchant = Merchant.create("merchant_001", "Acme", binding);

    assertEquals("merchant_001", merchant.id());
    assertEquals("env://ACME_MERCHANT_KEY", merchant.credentialRef());
    assertEquals("a1b2c3d4", merchant.credentialFingerprint());
    assertEquals(MerchantStatus.ACTIVE, merchant.status());
    assertEquals("default", merchant.tenantId());
  }

  @Test
  void channelBindsMerchantProviderAndCredentialReference() {
    var credential = CredentialBinding.create("env://HUIFU_RSA_PRIVATE_KEY", "ffeeddcc");
    var provider = ProviderBinding.create("huifu", credential);
    Channel channel =
        Channel.create("channel_001", "merchant_001", provider);

    assertEquals("merchant_001", channel.merchantId());
    assertEquals("huifu", channel.providerCode());
    assertEquals("env://HUIFU_RSA_PRIVATE_KEY", channel.credentialRef());
    assertEquals("ffeeddcc", channel.credentialFingerprint());
  }

  @Test
  void rejectsInvalidCredentialReferences() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CredentialBinding.create("kms://mmpay/merchant/001", "a1b2c3d4"));
  }

  @Test
  void rejectsReservedProviderCodes() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ProviderBinding.create("mock", CredentialBinding.create("env://MOCK_REF", "a1b2c3d4")));
  }
}
