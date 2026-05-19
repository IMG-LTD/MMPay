package com.imgltd.mmpay.audit;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class AuditHasher {
  private static final int MIN_KEY_BYTES = 32;
  private final byte[] hmacKey;

  public AuditHasher(byte[] hmacKey) {
    if (hmacKey.length < MIN_KEY_BYTES) {
      throw new IllegalArgumentException("audit key too short");
    }
    this.hmacKey = hmacKey.clone();
  }

  public String rowHmac(AuditEvent event, String previous) {
    return hmac(payload(event, previous));
  }

  public AuditVerifyResult verify(List<AuditEvent> candidateEvents) {
    String previous = null;
    for (AuditEvent event : candidateEvents) {
      var expected = rowHmac(event, previous);
      if (!expected.equals(event.rowHmac()) || !equalsNullable(previous, event.prevRowHmac())) {
        return new AuditVerifyResult(firstId(candidateEvents), lastId(candidateEvents), false, event.id());
      }
      previous = event.rowHmac();
    }
    return new AuditVerifyResult(firstId(candidateEvents), lastId(candidateEvents), true, null);
  }

  private String payload(AuditEvent event, String previous) {
    return event.id() + "|" + event.timestamp() + "|" + value(event.actorKind()) + "|" + value(event.actorId()) + "|"
        + value(event.action()) + "|" + value(event.targetKind()) + "|" + value(event.targetId()) + "|"
        + canonicalDetails(event.details()) + "|" + value(previous);
  }

  private String canonicalDetails(Map<String, ?> details) {
    return details.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .reduce((left, right) -> left + "," + right)
        .orElse("");
  }

  private String hmac(String payload) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("audit hmac failed", exception);
    }
  }

  private static boolean equalsNullable(String expected, String actual) {
    return expected == null ? actual == null : expected.equals(actual);
  }

  private static long firstId(List<AuditEvent> candidateEvents) {
    return candidateEvents.isEmpty() ? 0L : candidateEvents.getFirst().id();
  }

  private static long lastId(List<AuditEvent> candidateEvents) {
    return candidateEvents.isEmpty() ? 0L : candidateEvents.getLast().id();
  }

  private static String value(String text) {
    return text == null ? "" : text;
  }
}
