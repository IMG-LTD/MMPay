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
  void declaresInvoiceUnsupportedByCurrentProvider() {
    PaymentProviderAdapter adapter = new HuifuPaymentAdapter(validCredentialHandles());

    assertEquals(false, adapter.invoiceSupport().supported());
    assertEquals("unsupported by current provider", adapter.invoiceSupport().reason());
  }

  @Test
  void reportsMissingSandboxEnvironmentVariables() {
    Map<String, String> env = Map.of("HUIFU_MERCHANT_ID", "replace-with-merchant-id");

    IllegalStateException failure = assertThrows(IllegalStateException.class, () -> HuifuSandboxCredentials.from(env));

    assertTrue(failure.getMessage().contains("HUIFU_API_KEY"));
  }

  private static HuifuCredentialHandles validCredentialHandles() {
    return new HuifuCredentialHandles("kms://huifu/merchant-id", "kms://huifu/api-key", "kms://huifu/webhook");
  }
}
