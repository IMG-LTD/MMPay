package com.imgltd.mmpay.credentials;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Pattern;

public final class EnvironmentReferenceResolver {
  private static final int MAX_VALUE_BYTES = 64 * 1024;
  private static final Pattern ENV_REFERENCE = Pattern.compile("^env://([A-Z][A-Z0-9_]*)$");
  private final Map<String, String> environment;

  public EnvironmentReferenceResolver(Map<String, String> environment) {
    this.environment = Map.copyOf(environment);
  }

  public ResolvedCredentialReference resolve(String uri) {
    if (uri.startsWith("kms://")) {
      throw failure(ReferenceResolutionFailure.KMS_NO_RESOLVER, uri);
    }
    var matcher = ENV_REFERENCE.matcher(uri);
    if (!matcher.matches()) {
      throw failure(ReferenceResolutionFailure.SCHEME_INVALID, uri);
    }
    return resolveEnv(uri, matcher.group(1));
  }

  private ResolvedCredentialReference resolveEnv(String uri, String name) {
    var value = environment.get(name);
    if (value == null) {
      throw failure(ReferenceResolutionFailure.ENV_UNSET, uri);
    }
    var trimmed = value.stripTrailing();
    if (trimmed.isBlank()) {
      throw failure(ReferenceResolutionFailure.EMPTY_BYTES, uri);
    }
    if (trimmed.getBytes(StandardCharsets.UTF_8).length > MAX_VALUE_BYTES) {
      throw failure(ReferenceResolutionFailure.VALUE_TOO_LARGE, uri);
    }
    return new ResolvedCredentialReference(uri, fingerprint(trimmed));
  }

  private String fingerprint(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest).substring(0, 8);
    } catch (Exception exception) {
      throw new IllegalStateException("credential fingerprint failed", exception);
    }
  }

  private ReferenceResolutionException failure(ReferenceResolutionFailure failure, String uri) {
    return new ReferenceResolutionException(failure, uri);
  }
}
