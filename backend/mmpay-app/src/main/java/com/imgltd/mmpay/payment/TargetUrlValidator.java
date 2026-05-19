package com.imgltd.mmpay.payment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class TargetUrlValidator {
  void requireAcceptable(String targetUrl) {
    URI uri = parse(targetUrl);
    if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
      reject();
    }
    if (uri.getUserInfo() != null || uri.getRawFragment() != null) {
      reject();
    }
    if (isPrivateHost(uri.getHost())) {
      reject();
    }
  }

  private URI parse(String targetUrl) {
    try {
      return new URI(PaymentInput.requireText(targetUrl, "target_url"));
    } catch (URISyntaxException exception) {
      throw PaymentProblems.unprocessable(PaymentProblems.TARGET_URL_REJECTED, "target_url is invalid");
    }
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
    throw PaymentProblems.unprocessable(PaymentProblems.TARGET_URL_REJECTED, "target_url rejected by SSRF policy");
  }
}
