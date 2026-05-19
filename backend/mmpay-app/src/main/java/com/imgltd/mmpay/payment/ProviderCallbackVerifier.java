package com.imgltd.mmpay.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

public final class ProviderCallbackVerifier {
  private static final Duration MAX_SKEW = Duration.ofMinutes(5);
  private final String callbackSecret;
  private final Clock clock;

  public ProviderCallbackVerifier(@Value("${mmpay.provider-callback-secret:}") String callbackSecret, Clock clock) {
    this.callbackSecret = callbackSecret;
    this.clock = clock;
  }

  String requireValid(String signature, String body, Instant occurredAt) {
    if (callbackSecret == null || callbackSecret.isBlank()) {
      throw PaymentProblems.unauthorized(
          PaymentProblems.PROVIDER_CALLBACK_SIGNATURE_INVALID, "provider callback signing key is not configured");
    }
    if (outsideWindow(occurredAt)) {
      throw PaymentProblems.unauthorized(
          "urn:mmpay:problem:provider-callback-stale", "provider callback timestamp is stale");
    }
    return verifySignature(signature, body);
  }

  private String verifySignature(String signature, String body) {
    byte[] expected = hmac(body);
    byte[] actual = decode(signature);
    if (!MessageDigest.isEqual(expected, actual)) {
      throw PaymentProblems.unauthorized(
          PaymentProblems.PROVIDER_CALLBACK_SIGNATURE_INVALID, "provider callback signature is invalid");
    }
    return sha256(signature);
  }

  private boolean outsideWindow(Instant occurredAt) {
    Duration skew = Duration.between(occurredAt, Instant.now(clock)).abs();
    return skew.compareTo(MAX_SKEW) > 0;
  }

  private byte[] hmac(String body) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(callbackSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("provider callback signature verification failed", exception);
    }
  }

  private byte[] decode(String value) {
    try {
      return HexFormat.of().parseHex(PaymentInput.requireText(value, "signature"));
    } catch (IllegalArgumentException exception) {
      throw PaymentProblems.unauthorized(
          PaymentProblems.PROVIDER_CALLBACK_SIGNATURE_INVALID, "provider callback signature is invalid");
    }
  }

  private String sha256(String signature) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(signature.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("provider callback signature hash failed", exception);
    }
  }
}
