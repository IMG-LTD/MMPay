package com.imgltd.mmpay.setup;

import java.util.regex.Pattern;

public final class MmpayActuatorSanitizer {
  private static final Pattern SENSITIVE_NAME =
      Pattern.compile("(?i)(password|secret|key|token|signature|cert|pem|hmac|license_payload)");
  private static final String REDACTED = "******";

  public String sanitize(String name, String value) {
    if (SENSITIVE_NAME.matcher(name).find()) {
      return REDACTED;
    }
    return value;
  }
}
