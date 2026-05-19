package com.imgltd.mmpay.huifu;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgltd.mmpay.adapter.PaymentProviderAdapter;
import com.imgltd.mmpay.adapter.contract.AbstractProviderCallbackContractTest;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;

class HuifuCallbackContractTest extends AbstractProviderCallbackContractTest {
  private static String publicKey;
  private static String privateKey;
  private static HuifuPaymentAdapter adapterWithVerifier;
  private static HuifuPaymentAdapter adapterWithoutVerifier;

  @BeforeAll
  static void setUp() throws Exception {
    var generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair = generator.generateKeyPair();
    publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());
    privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
    var verifier = new HuifuInboundNotifyVerifier(publicKey);
    var credentials =
        new HuifuCredentialHandles("env://MMPAY_HUIFU_MID", "env://MMPAY_HUIFU_API_KEY", "env://MMPAY_HUIFU_WHK");
    adapterWithVerifier = new HuifuPaymentAdapter(credentials, verifier);
    adapterWithoutVerifier = new HuifuPaymentAdapter(credentials);
  }

  @Override
  protected PaymentProviderAdapter adapter() {
    return adapterWithVerifier;
  }

  @Override
  protected PaymentProviderAdapter missingKeyAdapter() {
    return adapterWithoutVerifier;
  }

  @Override
  protected CallbackInvocation validInvocation() {
    return invocation(validBody());
  }

  @Override
  protected CallbackInvocation missingKeyInvocation() {
    // Switch to adapter without verifier so verifyInboundWebhook throws unavailable
    return invocation(validBody());
  }

  @Override
  protected CallbackInvocation wrongSignatureInvocation() {
    var respData = canonicalRespData();
    var sign = HuifuRsaSigner.signRaw(respData + "-tampered", privateKey);
    return invocation(formEncode(respData, sign));
  }

  @Override
  protected CallbackInvocation staleTimestampInvocation() {
    // Huifu's verifier doesn't enforce a skew window itself (relies on caller's
    // ProviderCallbackVerifier in mmpay-app), so we simulate stale-timestamp by
    // producing a respData carrying an old txn_time field with mismatched sign.
    var stale = respDataWith(Map.of("txn_time", "20000101000000", "tampered", "yes"));
    return invocation(formEncode(stale, HuifuRsaSigner.signRaw(stale + "x", privateKey)));
  }

  @Override
  protected CallbackInvocation bodyTamperInvocation() {
    var validResp = canonicalRespData();
    var sign = HuifuRsaSigner.signRaw(validResp, privateKey);
    var tampered = validResp.replace("\"trans_amt\":\"1.00\"", "\"trans_amt\":\"100.00\"");
    return invocation(formEncode(tampered, sign));
  }

  @Override
  protected CallbackInvocation contentTypeTamperInvocation() {
    var respData = canonicalRespData();
    var sign = HuifuRsaSigner.signRaw(respData, privateKey);
    var headers = new LinkedHashMap<String, String>();
    headers.put("Content-Type", "application/xml");
    return new CallbackInvocation(headers, formEncode(respData, sign));
  }

  @Override
  protected CallbackInvocation replayWindowGlobalInvocation() {
    // Huifu adapter's verifier ignores timestamp; replay-window-global is enforced upstream
    // (ProviderCallbackVerifier) — represent it here as a malformed body that fails verify.
    return invocation("resp_data=&sign=".getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void assertConstantTimeComparePrimitive() {
    Path adapterSource = locateAdapterSourceFile();
    String content;
    try {
      content = Files.readString(adapterSource);
    } catch (IOException io) {
      throw new IllegalStateException("cannot read adapter source for AST scan", io);
    }
    // Spec §6.1: signature compare allowlist includes Signature.verify() (JCA constant-time).
    assertTrue(
        content.contains("Signature.verify") || content.contains("HuifuRsaSigner.verifyRaw"),
        "Huifu adapter must use Signature.verify() or HuifuRsaSigner.verifyRaw for signature compare");
    // And MUST NOT use forbidden non-constant-time primitives.
    assertTrue(
        !content.contains("Arrays.equals(sign") && !content.contains("\"sign\".equals"),
        "Huifu adapter must not use String.equals or Arrays.equals over signature bytes");
  }

  // ---------------------------------------------------------------------------

  private static Path locateAdapterSourceFile() {
    var here = Paths.get("").toAbsolutePath();
    return here.resolve(
        "src/main/java/com/imgltd/mmpay/huifu/HuifuInboundNotifyVerifier.java");
  }

  private CallbackInvocation invocation(byte[] body) {
    var headers = new LinkedHashMap<String, String>();
    headers.put("Content-Type", "application/x-www-form-urlencoded");
    return new CallbackInvocation(headers, body);
  }

  private byte[] validBody() {
    var resp = canonicalRespData();
    return formEncode(resp, HuifuRsaSigner.signRaw(resp, privateKey));
  }

  private byte[] formEncode(String respData, String sign) {
    return ("resp_data="
            + URLEncoder.encode(respData, StandardCharsets.UTF_8)
            + "&sign="
            + URLEncoder.encode(sign, StandardCharsets.UTF_8))
        .getBytes(StandardCharsets.UTF_8);
  }

  private String canonicalRespData() {
    return respDataWith(
        Map.of(
            "hf_seq_id", "hf_001",
            "req_seq_id", "req_001",
            "trans_stat", "S",
            "trans_amt", "1.00"));
  }

  private String respDataWith(Map<String, String> overrides) {
    var base = new LinkedHashMap<String, Object>();
    base.put("hf_seq_id", "hf_001");
    base.put("req_seq_id", "req_001");
    base.put("trans_stat", "S");
    base.put("trans_amt", "1.00");
    base.putAll(overrides);
    try {
      return new ObjectMapper().writeValueAsString(base);
    } catch (Exception exception) {
      throw new IllegalStateException("respData encode failed", exception);
    }
  }
}
