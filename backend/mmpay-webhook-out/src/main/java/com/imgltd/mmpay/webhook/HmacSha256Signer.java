package com.imgltd.mmpay.webhook;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

final class HmacSha256Signer {
  static final String ALGORITHM = "HmacSHA256";

  private final SecretKeySpec secretKey;

  HmacSha256Signer(String secret) {
    if (secret == null || secret.isBlank()) {
      throw new IllegalArgumentException("webhook secret must not be blank");
    }
    this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
  }

  String signHex(String body) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(secretKey);
      return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("failed to sign webhook payload", exception);
    }
  }
}
