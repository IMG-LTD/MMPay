package com.imgltd.mmpay.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

public final class WebhookSigner {
  static final String SIGNATURE_VERSION = "v1";
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  private final HmacSha256Signer hmacSigner;
  private final Clock clock;

  private WebhookSigner(String secret, Clock clock) {
    this.hmacSigner = new HmacSha256Signer(secret);
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public static WebhookSigner withSecret(String secret) {
    return new WebhookSigner(secret, Clock.systemUTC());
  }

  public static WebhookSigner withSecretAndClock(String secret, Clock clock) {
    return new WebhookSigner(secret, clock);
  }

  public WebhookMessage sign(Map<String, ?> payload) {
    rejectLicenseClaimFields(payload);
    var body = toJson(payload);
    var unixSeconds = clock.instant().getEpochSecond();
    var canonical = canonicalInput(payload, body, unixSeconds);
    var hex = hmacSigner.signHex(canonical);
    var header = "t=" + unixSeconds + ",v1=" + hex;
    return new WebhookMessage(body, header);
  }

  /**
   * Canonical signing input per spec §5.1: newline-joined 7-tuple. body sha256 makes
   * amount/currency tampering detectable; `t` claim defends against header replay.
   */
  private static String canonicalInput(Map<String, ?> payload, String body, long unixSeconds) {
    var sha256 = HexFormat.of().formatHex(sha256(body));
    return SIGNATURE_VERSION
        + "\n"
        + unixSeconds
        + "\n"
        + stringField(payload, "eventId")
        + "\n"
        + stringField(payload, "paymentIntentId")
        + "\n"
        + stringField(payload, "status")
        + "\n"
        + stringField(payload, "occurredAt")
        + "\n"
        + sha256;
  }

  private static String stringField(Map<String, ?> payload, String key) {
    var value = payload.get(key);
    return value == null ? "" : value.toString();
  }

  private static byte[] sha256(String body) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(body.getBytes(StandardCharsets.UTF_8));
    } catch (Exception exception) {
      throw new IllegalStateException("sha256 unavailable", exception);
    }
  }

  private static void rejectLicenseClaimFields(Map<String, ?> payload) {
    for (var field : LicenseClaimFieldNames.prohibitedFields()) {
      if (payload.containsKey(field)) {
        throw new IllegalArgumentException(
            "Webhook payload must not contain license claim field: " + field);
      }
    }
  }

  private static String toJson(Map<String, ?> payload) {
    try {
      return OBJECT_MAPPER.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("webhook payload is not serializable", exception);
    }
  }
}
