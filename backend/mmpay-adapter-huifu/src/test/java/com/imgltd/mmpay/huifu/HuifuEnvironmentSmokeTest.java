package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.adapter.ProviderPaymentRequest;
import com.imgltd.mmpay.adapter.ProviderRefundRequest;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "MMPAY_HUIFU_ENV_SMOKE", matches = "true")
class HuifuEnvironmentSmokeTest {

  @Test
  void loadsEnvironmentCredentialsAndSignsPreparedRequests() {
    HuifuSandboxCredentials credentials = HuifuSandboxCredentials.from(System.getenv());
    HuifuPaymentRequestFactory factory = new HuifuPaymentRequestFactory(credentials);
    ProviderPaymentRequest payment =
        new ProviderPaymentRequest(
            "mmpay-env-smoke-1",
            101,
            "CNY",
            "MMPay Huifu env smoke",
            "https://localhost/return",
            credentials.notifyUrl());
    ProviderRefundRequest refund = new ProviderRefundRequest("mmpay-env-smoke-refund-1", 101, "env smoke refund");

    HuifuSignedRequest create = factory.createAggregationNativePayment(payment, LocalDate.of(2026, 5, 18), "202605180101");
    HuifuSignedRequest query =
        factory.queryAggregationPayment(LocalDate.of(2026, 5, 18), "202605180102", "20260518", "202605180101");
    HuifuSignedRequest refundRequest =
        factory.refundAggregationPayment(refund, LocalDate.of(2026, 5, 18), "202605180103", "20260518", "202605180101");

    assertSigned(create, credentials);
    assertSigned(query, credentials);
    assertSigned(refundRequest, credentials);
    assertPlatformPublicKeyIsParseable(credentials.rsaPublicKey());
    assertEquals("env://HUIFU_RSA_PRIVATE_KEY", credentials.toCredentialHandles().apiKeyHandle());
    assertEquals("RECV_ORD_ID_202605180101", HuifuInboundNotifyVerifier.acknowledge("202605180101"));
  }

  private static void assertSigned(HuifuSignedRequest request, HuifuSandboxCredentials credentials) {
    assertEquals(credentials.sysId(), request.envelope().get("sys_id"));
    assertEquals(credentials.productId(), request.envelope().get("product_id"));
    assertEquals(credentials.merchantId(), request.data().get("huifu_id"));
    assertTrue(request.sign().length() > 128);
  }

  private static void assertPlatformPublicKeyIsParseable(String publicKey) {
    String invalidSignature = Base64.getEncoder().encodeToString(new byte[256]);
    assertFalse(HuifuRsaSigner.verifyRaw("mmpay-huifu-env-smoke", publicKey, invalidSignature));
  }
}
