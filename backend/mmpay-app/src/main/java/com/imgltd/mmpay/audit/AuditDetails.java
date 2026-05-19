package com.imgltd.mmpay.audit;

import java.util.Map;
import java.util.regex.Pattern;

public final class AuditDetails {
  public static final String FORBIDDEN_KEY_REGEX =
      "(?i)(password|secret|key|token|signature|hash|cert|pem|license_payload)";

  private static final Pattern FORBIDDEN_KEY_PATTERN = Pattern.compile(FORBIDDEN_KEY_REGEX);

  private AuditDetails() {}

  public static void validate(Map<String, ?> details) {
    validateMap(details);
  }

  private static void validateMap(Map<?, ?> details) {
    for (var entry : details.entrySet()) {
      var key = String.valueOf(entry.getKey());
      validateKey(key);
      validateValue(entry.getValue());
    }
  }

  private static void validateValue(Object value) {
    if (value instanceof Map<?, ?> nested) {
      validateMap(nested);
      return;
    }
    if (value instanceof Iterable<?> items) {
      for (Object item : items) {
        validateValue(item);
      }
    }
  }

  private static void validateKey(String key) {
    if (FORBIDDEN_KEY_PATTERN.matcher(key).find()) {
      throw new AuditDetailsLeakException(key);
    }
  }
}
