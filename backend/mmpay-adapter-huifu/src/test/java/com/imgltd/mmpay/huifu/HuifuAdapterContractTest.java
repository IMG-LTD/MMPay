package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.adapter.PaymentProviderAdapter;
import com.imgltd.mmpay.adapter.ProviderCapability;
import com.imgltd.mmpay.adapter.ProviderOperationUnavailableException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HuifuAdapterContractTest {

  @Test
  void exposesHuifuProviderCapabilitiesThroughAdapterSpi() {
    PaymentProviderAdapter adapter = new HuifuPaymentAdapter(validCredentialHandles());

    assertInstanceOf(PaymentProviderAdapter.class, adapter);
    assertEquals("huifu", adapter.providerCode());
    assertTrue(adapter.capabilities().contains(ProviderCapability.CREATE_PAYMENT));
    assertTrue(adapter.capabilities().contains(ProviderCapability.QUERY_PAYMENT));
    assertTrue(adapter.capabilities().contains(ProviderCapability.REFUND));
    assertTrue(adapter.capabilities().contains(ProviderCapability.VERIFY_INBOUND_WEBHOOK));
  }

  @Test
  void refusesBlankCredentialHandles() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new HuifuCredentialHandles("kms://huifu/merchant", " ", "kms://huifu/webhook"));
  }

  @Test
  void exposesUnavailableOperationsAsExplicitErrors() {
    PaymentProviderAdapter adapter = new HuifuPaymentAdapter(validCredentialHandles());

    ProviderOperationUnavailableException failure =
        assertThrows(ProviderOperationUnavailableException.class, () -> adapter.queryPayment("hf-order-1"));

    assertEquals("huifu", failure.providerCode());
    assertEquals("queryPayment", failure.operation());
  }

  @Test
  void declaresRuntimeUnavailableUntilLiveProviderClientIsWired() {
    PaymentProviderAdapter adapter = new HuifuPaymentAdapter(validCredentialHandles());

    assertEquals(false, adapter.runtimeStatus().available());
    assertEquals("live provider client is not wired", adapter.runtimeStatus().reason());
  }

  @Test
  void declaresInvoiceUnsupportedByCurrentProvider() {
    PaymentProviderAdapter adapter = new HuifuPaymentAdapter(validCredentialHandles());

    assertEquals(false, adapter.invoiceSupport().supported());
    assertEquals("unsupported by current provider", adapter.invoiceSupport().reason());
  }

  @Test
  void reportsMissingSandboxEnvironmentVariables() {
    Map<String, String> env = Map.of("HUIFU_MERCHANT_ID", "replace-with-merchant-id");

    IllegalStateException failure = assertThrows(IllegalStateException.class, () -> HuifuSandboxCredentials.from(env));

    assertTrue(failure.getMessage().contains("HUIFU_SYS_ID"));
  }

  @Test
  void allowsMissingWebhookEndpointKeyBecauseControlPanelWebhookIsOptional() {
    Map<String, String> env =
        Map.of(
            "HUIFU_SYS_ID", "sys",
            "HUIFU_PRODUCT_ID", "prod",
            "HUIFU_RSA_PUBLIC_KEY", "pub",
            "HUIFU_RSA_PRIVATE_KEY", "priv",
            "HUIFU_SKILL_SOURCE", "hfps/1.2.0",
            "HUIFU_MERCHANT_ID", "mid",
            "HUIFU_NOTIFY_URL", "https://callback.test");

    HuifuSandboxCredentials credentials = HuifuSandboxCredentials.from(env);

    assertEquals("", credentials.webhookEndpointKey());
  }

  private static HuifuCredentialHandles validCredentialHandles() {
    return new HuifuCredentialHandles(
        "kms://huifu/merchant-id", "kms://huifu/rsa-private-key", "kms://huifu/webhook-endpoint-key");
  }
}
