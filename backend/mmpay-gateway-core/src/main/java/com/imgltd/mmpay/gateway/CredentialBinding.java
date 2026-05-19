package com.imgltd.mmpay.gateway;

import java.util.regex.Pattern;

public record CredentialBinding(String credentialRef, String credentialFingerprint) {
  private static final Pattern CREDENTIAL_REF_PATTERN =
      Pattern.compile("^env://[A-Z][A-Z0-9_]{2,127}$");
  private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("[0-9a-f]{8}");

  public CredentialBinding {
    credentialRef = requirePattern(credentialRef, "credentialRef", CREDENTIAL_REF_PATTERN);
    credentialFingerprint =
        requirePattern(credentialFingerprint, "credentialFingerprint", FINGERPRINT_PATTERN);
  }

  public static CredentialBinding create(String credentialRef, String credentialFingerprint) {
    return new CredentialBinding(credentialRef, credentialFingerprint);
  }

  private static String requirePattern(String value, String fieldName, Pattern pattern) {
    var text = DomainChecks.requireText(value, fieldName);
    if (!pattern.matcher(text).matches()) {
      throw new IllegalArgumentException(fieldName + " is invalid");
    }
    return text;
  }
}
