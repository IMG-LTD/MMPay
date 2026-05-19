package com.imgltd.mmpay.iam;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class BasicClientCredentials {
  private static final String BASIC_PREFIX = "Basic ";

  private BasicClientCredentials() {}

  static ClientCredentials parse(String authorization) {
    if (authorization == null || !authorization.startsWith(BASIC_PREFIX)) {
      throw invalidClient();
    }
    var decoded = decode(authorization.substring(BASIC_PREFIX.length()));
    var separator = decoded.indexOf(':');
    if (separator < 1 || separator == decoded.length() - 1) {
      throw invalidClient();
    }
    return new ClientCredentials(decoded.substring(0, separator), decoded.substring(separator + 1));
  }

  private static ResponseStatusException invalidClient() {
    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_client");
  }

  private static String decode(String encoded) {
    try {
      return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException exception) {
      throw invalidClient();
    }
  }
}
