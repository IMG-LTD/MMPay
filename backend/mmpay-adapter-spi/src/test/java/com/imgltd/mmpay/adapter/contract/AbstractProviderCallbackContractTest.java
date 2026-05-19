package com.imgltd.mmpay.adapter.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.imgltd.mmpay.adapter.PaymentProviderAdapter;
import com.imgltd.mmpay.adapter.ProviderEvent;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Normative 8-case provider-callback contract test every {@link PaymentProviderAdapter}
 * implementation MUST extend (spec P3 §6.1).
 *
 * <p>The abstract base names the 8 cases. Each case calls a hook the subclass implements with
 * provider-specific signing details. Subclass hooks return concrete invocation inputs (headers +
 * body) so the base test can drive the adapter under test without knowing the signing primitive
 * (HMAC, RSA, form-encoded, etc.).
 */
public abstract class AbstractProviderCallbackContractTest {

  /** Provide a freshly-configured adapter under test. Implementation MAY cache. */
  protected abstract PaymentProviderAdapter adapter();

  /**
   * Provide an adapter constructed WITHOUT a signing key wired (so verify must fail closed).
   * Defaults to the regular adapter; subclasses with a separate construction path SHOULD override.
   */
  protected PaymentProviderAdapter missingKeyAdapter() {
    return adapter();
  }

  /** Inputs the adapter accepts as a valid callback in the happy case. */
  protected abstract CallbackInvocation validInvocation();

  /** Same inputs but with the signing key unset; adapter must fail closed. */
  protected abstract CallbackInvocation missingKeyInvocation();

  /** Valid inputs except the signature is tampered. */
  protected abstract CallbackInvocation wrongSignatureInvocation();

  /** Valid inputs except the timestamp / occurredAt is older than the configured skew window. */
  protected abstract CallbackInvocation staleTimestampInvocation();

  /** Valid signature over a different body; the body sent here must trip the verifier. */
  protected abstract CallbackInvocation bodyTamperInvocation();

  /** Valid inputs with a swapped Content-Type header value. */
  protected abstract CallbackInvocation contentTypeTamperInvocation();

  /**
   * Valid inputs whose timestamp is so old that any "global" replay window rejects them even on
   * first delivery.
   */
  protected abstract CallbackInvocation replayWindowGlobalInvocation();

  /**
   * Subclasses MUST assert that the adapter implementation uses a constant-time signature compare
   * primitive (e.g., {@code MessageDigest.isEqual}, {@code Signature.verify()}). The CI-side AST
   * scan is the real defense; this method makes the requirement visible at the test layer.
   */
  protected abstract void assertConstantTimeComparePrimitive();

  // --- Case 1: missing_key_fail_closed ---------------------------------------
  @Test
  @DisplayName("missing_key_fail_closed")
  void missingKeyFailClosed() {
    var invocation = missingKeyInvocation();
    var noKey = missingKeyAdapter();
    try {
      var result = noKey.verifyInboundWebhook(invocation.headers(), invocation.body());
      if (result == null) {
        return;
      }
      fail("missing key must fail closed — got " + result);
    } catch (RuntimeException expected) {
      // accepted rejection
    }
  }

  // --- Case 2: wrong_signature_rejected --------------------------------------
  @Test
  @DisplayName("wrong_signature_rejected")
  void wrongSignatureRejected() {
    assertRejects("tampered signature must be rejected", wrongSignatureInvocation());
  }

  // --- Case 3: replay_idempotent ---------------------------------------------
  @Test
  @DisplayName("replay_idempotent")
  void replayIdempotent() {
    var invocation = validInvocation();
    var first = adapter().verifyInboundWebhook(invocation.headers(), invocation.body());
    var second = adapter().verifyInboundWebhook(invocation.headers(), invocation.body());
    assertNotNull(first, "first invocation must verify");
    assertNotNull(second, "replay must also verify (idempotent)");
    assertEquals(first.eventId(), second.eventId(), "replay must surface the same eventId");
  }

  // --- Case 4: stale_timestamp_rejected --------------------------------------
  @Test
  @DisplayName("stale_timestamp_rejected")
  void staleTimestampRejected() {
    assertRejects("stale timestamp must be rejected", staleTimestampInvocation());
  }

  // --- Case 5: body_tamper_rejected ------------------------------------------
  @Test
  @DisplayName("body_tamper_rejected")
  void bodyTamperRejected() {
    assertRejects("tampered body must be rejected", bodyTamperInvocation());
  }

  // --- Case 6: constant_time_compare_enforced --------------------------------
  @Test
  @DisplayName("constant_time_compare_enforced")
  void constantTimeCompareEnforced() {
    assertConstantTimeComparePrimitive();
  }

  // --- Case 7: content_type_tamper_rejected ----------------------------------
  @Test
  @DisplayName("content_type_tamper_rejected")
  void contentTypeTamperRejected() {
    var invocation = contentTypeTamperInvocation();
    // Two acceptable outcomes per spec: either the adapter verifies (canonicalization ignores
    // Content-Type) OR rejects (canonicalization is content-type-aware). What it MUST NOT do is
    // bypass signature verification because the Content-Type changed.
    try {
      var event = adapter().verifyInboundWebhook(invocation.headers(), invocation.body());
      assertNotNull(event, "successful verify under content-type tamper must still return event");
    } catch (RuntimeException expectedReject) {
      // accepted — content-type-aware canonicalization rejected
    }
  }

  // --- Case 8: replay_window_global ------------------------------------------
  @Test
  @DisplayName("replay_window_global")
  void replayWindowGlobal() {
    assertRejects("very-old timestamp must be rejected globally", replayWindowGlobalInvocation());
  }

  // --- Helpers ---------------------------------------------------------------
  private void assertRejects(String message, CallbackInvocation invocation) {
    try {
      var result = adapter().verifyInboundWebhook(invocation.headers(), invocation.body());
      if (result == null) {
        return;
      }
      fail(message + " — got " + result);
    } catch (RuntimeException expected) {
      // accepted rejection
    }
  }

  /** Adapter-specific invocation inputs (headers + raw body). */
  public record CallbackInvocation(Map<String, String> headers, byte[] body) {
    public CallbackInvocation {
      java.util.Objects.requireNonNull(headers, "headers");
      java.util.Objects.requireNonNull(body, "body");
    }
  }

  // Reference the SPI type so the test-jar carries the dependency transitively.
  @SuppressWarnings("unused")
  private static final Class<?> PROVIDER_EVENT_TYPE = ProviderEvent.class;
}
