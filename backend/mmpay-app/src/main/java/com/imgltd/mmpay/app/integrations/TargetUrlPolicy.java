package com.imgltd.mmpay.app.integrations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class TargetUrlPolicy {
  private final boolean allowLocalTargets;

  TargetUrlPolicy(boolean allowLocalTargets) {
    this.allowLocalTargets = allowLocalTargets;
  }

  URI requireAcceptable(String targetUrl) {
    URI uri = parse(targetUrl);
    if (uri.getHost() == null || uri.getUserInfo() != null || uri.getRawFragment() != null) {
      reject();
    }
    if (!schemeAllowed(uri) || (!allowLocalTargets && isPrivateHost(uri.getHost()))) {
      reject();
    }
    return uri;
  }

  private URI parse(String targetUrl) {
    try {
      return new URI(IntegrationInput.requireText(targetUrl, "target_url"));
    } catch (URISyntaxException exception) {
      throw IntegrationInput.problem("urn:mmpay:problem:target-url-rejected", "target_url is invalid");
    }
  }

  private boolean schemeAllowed(URI uri) {
    String scheme = uri.getScheme();
    return "https".equalsIgnoreCase(scheme) || (allowLocalTargets && "http".equalsIgnoreCase(scheme));
  }

  private boolean isPrivateHost(String host) {
    String normalized = host.toLowerCase(Locale.ROOT);
    return normalized.equals("localhost")
        || normalized.startsWith("127.")
        || normalized.startsWith("10.")
        || normalized.startsWith("192.168.")
        || normalized.startsWith("169.254.")
        || normalized.matches("172\\.(1[6-9]|2[0-9]|3[0-1])\\..*")
        || normalized.equals("::1")
        || normalized.startsWith("fd")
        || normalized.startsWith("fe80:");
  }

  private void reject() {
    throw IntegrationInput.problem("urn:mmpay:problem:target-url-rejected", "target_url rejected by SSRF policy");
  }
}
