package com.imgltd.mmpay.credentials;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

public final class EnvironmentReferenceResolver {
  private static final int MAX_VALUE_BYTES = 64 * 1024;
  private static final Pattern ENV_REFERENCE = Pattern.compile("^env://([A-Z][A-Z0-9_]*)$");
  private static final Pattern ENV_B64_REFERENCE = Pattern.compile("^env-b64://([A-Z][A-Z0-9_]*)$");
  private final Map<String, String> environment;

  public EnvironmentReferenceResolver(Map<String, String> environment) {
    this.environment = Map.copyOf(environment);
  }

  public ResolvedCredentialReference resolve(String uri) {
    if (uri.startsWith("kms://")) {
      throw failure(ReferenceResolutionFailure.KMS_NO_RESOLVER, uri);
    }
    var envMatcher = ENV_REFERENCE.matcher(uri);
    if (envMatcher.matches()) {
      return resolveEnv(uri, envMatcher.group(1), false);
    }
    var b64Matcher = ENV_B64_REFERENCE.matcher(uri);
    if (b64Matcher.matches()) {
      return resolveEnv(uri, b64Matcher.group(1), true);
    }
    throw failure(ReferenceResolutionFailure.SCHEME_INVALID, uri);
  }

  /**
   * Resolve an env-backed reference. When {@code base64Decode} is true, decode the env value as
   * base64 (multi-line PEM tolerant; spec P4 §1.1.2 calls this out for vendor mTLS CA bundles
   * that YAML strips newlines from).
   */
  private ResolvedCredentialReference resolveEnv(String uri, String name, boolean base64Decode) {
    var raw = environment.get(name);
    if (raw == null) {
      throw failure(ReferenceResolutionFailure.ENV_UNSET, uri);
    }
    var trimmed = raw.stripTrailing();
    if (trimmed.isBlank()) {
      throw failure(ReferenceResolutionFailure.EMPTY_BYTES, uri);
    }
    byte[] bytes;
    if (base64Decode) {
      try {
        bytes = Base64.getDecoder().decode(trimmed);
      } catch (IllegalArgumentException exception) {
        throw failure(ReferenceResolutionFailure.SCHEME_INVALID, uri);
      }
    } else {
      bytes = trimmed.getBytes(StandardCharsets.UTF_8);
    }
    if (bytes.length > MAX_VALUE_BYTES) {
      throw failure(ReferenceResolutionFailure.VALUE_TOO_LARGE, uri);
    }
    return new ResolvedCredentialReference(uri, fingerprint(bytes));
  }

  private String fingerprint(byte[] bytes) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      return HexFormat.of().formatHex(digest).substring(0, 8);
    } catch (Exception exception) {
      throw new IllegalStateException("credential fingerprint failed", exception);
    }
  }

  private ReferenceResolutionException failure(ReferenceResolutionFailure failure, String uri) {
    return new ReferenceResolutionException(failure, uri);
  }
}
