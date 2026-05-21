package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.adapter.ProviderEvent;
import com.imgltd.mmpay.adapter.ProviderPaymentRequest;
import com.imgltd.mmpay.adapter.ProviderPaymentStatus;
import com.imgltd.mmpay.adapter.ProviderRefundRequest;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HuifuSignedRequestTest {

  @Test
  void buildsSignedAggregationNativePaymentEnvelopeWithSkillHeaders() throws Exception {
    KeyPair keyPair = generateKeyPair();
    HuifuSandboxCredentials credentials = credentials(keyPair);
    HuifuPaymentRequestFactory factory = new HuifuPaymentRequestFactory(credentials);
    ProviderPaymentRequest request =
        new ProviderPaymentRequest("mmpay-order-1", 1234, "CNY", "MMPay test goods", "https://return.test", "https://ignored.test");

    HuifuSignedRequest signed =
        factory.createAggregationNativePayment(request, LocalDate.of(2026, 5, 18), "202605180001");

    assertEquals("test-sys-id", signed.envelope().get("sys_id"));
    assertEquals("test-product-id", signed.envelope().get("product_id"));
    assertEquals("javaSDK_lightning_1.0.5", signed.headers().get("sdk_version"));
    assertEquals("javaSDK_lightning_1.0.5", signed.headers().get("jpt-sdk_version"));
    assertEquals("test-sys-id", signed.headers().get("sys_id"));
    assertEquals("test-sys-id", signed.headers().get("jpt-sys_id"));
    assertEquals("hfps/1.2.0", signed.headers().get("jpt-x-skill-source"));
    assertEquals("test-merchant-id", signed.headers().get("jpt-x-skill-huifu_id"));
    assertEquals("12.34", signed.data().get("trans_amt"));
    assertEquals("A_NATIVE", signed.data().get("trade_type"));
    assertTrue(HuifuRsaSigner.verifyData(signed.data(), credentials.rsaPublicKey(), signed.sign()));
  }

  @Test
  void verifiesNotifyUrlPayloadAndMapsPaymentStatus() throws Exception {
    KeyPair keyPair = generateKeyPair();
    HuifuSandboxCredentials credentials = credentials(keyPair);
    HuifuInboundNotifyVerifier verifier = new HuifuInboundNotifyVerifier(credentials.rsaPublicKey());
    String respData =
        "{\"hf_seq_id\":\"hf-1\",\"req_seq_id\":\"202605180001\",\"trans_stat\":\"S\",\"trans_amt\":\"12.34\"}";
    String sign = HuifuRsaSigner.signRaw(respData, credentials.rsaPrivateKey());
    byte[] rawBody = ("resp_data=" + urlEncode(respData) + "&sign=" + urlEncode(sign)).getBytes();

    ProviderEvent event = verifier.verify(rawBody);

    assertEquals("hf-1", event.eventId());
    assertEquals("202605180001", event.providerOrderId());
    assertEquals(ProviderPaymentStatus.SUCCEEDED, event.status());
    assertEquals(1234, event.amountMinor());
  }

  @Test
  void buildsSignedAggregationQueryEnvelopeWithOriginalPaymentReference() throws Exception {
    KeyPair keyPair = generateKeyPair();
    HuifuSandboxCredentials credentials = credentials(keyPair);
    HuifuPaymentRequestFactory factory = new HuifuPaymentRequestFactory(credentials);

    HuifuSignedRequest signed = factory.queryAggregationPayment("20260517", "origin-pay-seq-1");

    assertEquals("test-sys-id", signed.envelope().get("sys_id"));
    assertEquals("test-product-id", signed.envelope().get("product_id"));
    assertEquals("javaSDK_lightning_1.0.5", signed.headers().get("sdk_version"));
    assertEquals("test-sys-id", signed.headers().get("jpt-sys_id"));
    assertEquals("hfps/1.2.0", signed.headers().get("jpt-x-skill-source"));
    assertEquals("test-merchant-id", signed.headers().get("jpt-x-skill-huifu_id"));
    assertEquals("test-merchant-id", signed.data().get("huifu_id"));
    assertEquals("20260517", signed.data().get("req_date"));
    assertEquals("origin-pay-seq-1", signed.data().get("req_seq_id"));
    assertNull(signed.data().get("org_req_date"));
    assertNull(signed.data().get("org_req_seq_id"));
    assertTrue(HuifuRsaSigner.verifyData(signed.data(), credentials.rsaPublicKey(), signed.sign()));
  }

  @Test
  void buildsSignedAggregationRefundEnvelopeWithOriginalPaymentReference() throws Exception {
    KeyPair keyPair = generateKeyPair();
    HuifuSandboxCredentials credentials = credentials(keyPair);
    HuifuPaymentRequestFactory factory = new HuifuPaymentRequestFactory(credentials);
    ProviderRefundRequest request = new ProviderRefundRequest("refund-order-1", 345, "customer request");

    HuifuSignedRequest signed =
        factory.refundAggregationPayment(
            request, LocalDate.of(2026, 5, 18), "202605180003", "20260517", "origin-pay-seq-1");

    assertEquals("3.45", signed.data().get("ord_amt"));
    assertEquals("customer request", signed.data().get("remark"));
    assertEquals("http://localhost:8000/notify.php", signed.data().get("notify_url"));
    assertEquals("20260518", signed.data().get("req_date"));
    assertEquals("202605180003", signed.data().get("req_seq_id"));
    assertEquals("test-merchant-id", signed.data().get("huifu_id"));
    assertEquals("20260517", signed.data().get("org_req_date"));
    assertEquals("origin-pay-seq-1", signed.data().get("org_req_seq_id"));
    assertTrue(HuifuRsaSigner.verifyData(signed.data(), credentials.rsaPublicKey(), signed.sign()));
  }

  @Test
  void buildsNotifyUrlAcknowledgementFromRequestSequenceId() {
    assertEquals("RECV_ORD_ID_202605180001", HuifuInboundNotifyVerifier.acknowledge("202605180001"));
  }

  private static HuifuSandboxCredentials credentials(KeyPair keyPair) {
    return new HuifuSandboxCredentials(
        "test-sys-id",
        "test-product-id",
        encode(keyPair.getPublic().getEncoded()),
        encode(keyPair.getPrivate().getEncoded()),
        "hfps/1.2.0",
        "test-merchant-id",
        "http://localhost:8000/notify.php",
        "replace-with-webhook-endpoint-key",
        "");
  }

  private static KeyPair generateKeyPair() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    return generator.generateKeyPair();
  }

  private static String encode(byte[] value) {
    return Base64.getEncoder().encodeToString(value);
  }

  private static String urlEncode(String value) {
    return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
  }
}
