package com.imgltd.mmpay.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MerchantChannelTest {
  @Test
  void merchantStoresCredentialHandleOnly() {
    Merchant merchant = Merchant.create("merchant_001", "MMMail", "kms://mmpay/merchant/001");

    assertEquals("merchant_001", merchant.id());
    assertEquals("kms://mmpay/merchant/001", merchant.credentialHandle());
  }

  @Test
  void channelBindsMerchantProviderAndCredentialHandle() {
    Channel channel =
        Channel.create("channel_001", "merchant_001", "huifu", "kms://mmpay/channel/huifu");

    assertEquals("merchant_001", channel.merchantId());
    assertEquals("huifu", channel.providerCode());
    assertEquals("kms://mmpay/channel/huifu", channel.credentialHandle());
  }

  @Test
  void merchantRejectsBlankCredentialHandles() {
    assertThrows(
        IllegalArgumentException.class, () -> Merchant.create("merchant_bad", "MMMail", " "));
  }
}
