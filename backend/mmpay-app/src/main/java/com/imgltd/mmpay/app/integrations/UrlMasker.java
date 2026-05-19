package com.imgltd.mmpay.app.integrations;

import java.net.URI;
import java.net.URISyntaxException;

final class UrlMasker {
  private UrlMasker() {}

  static String mask(String targetUrl) {
    try {
      URI uri = new URI(targetUrl);
      return uri.getScheme() + "://" + uri.getRawAuthority() + "/[REDACTED]";
    } catch (URISyntaxException exception) {
      return "[INVALID]";
    }
  }
}
