package com.imgltd.mmpay.auditverifier;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class RestoreAttestationHmacVerifier implements RestoreAttestationVerifier {
  private static final String ALGORITHM = "HmacSHA256";
  private static final int MIN_KEY_BYTES = 32;
  private final byte[] key;

  public RestoreAttestationHmacVerifier(byte[] key) {
    if (key.length < MIN_KEY_BYTES) {
      throw new IllegalArgumentException("restore attestation key too short");
    }
    this.key = key.clone();
  }

  public static String computeHex(byte[] key, Map<String, String> details) {
    try {
      var mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(key.clone(), ALGORITHM));
      return HexFormat.of().formatHex(mac.doFinal(payload(details).getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("restore attestation hmac failed", exception);
    }
  }

  public static String payload(Map<String, String> details) {
    return details.get("prev_segment_terminal_row_hmac")
        + details.get("backup_file_sha256")
        + details.get("snapshot_timestamp")
        + details.get("restore_nonce");
  }

  @Override
  public boolean verify(Map<String, String> details) {
    var expected = computeHex(key, details);
    var actual = details.get("attestation_hmac");
    return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
