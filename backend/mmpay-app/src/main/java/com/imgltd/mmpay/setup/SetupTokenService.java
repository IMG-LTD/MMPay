package com.imgltd.mmpay.setup;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.Supplier;

public final class SetupTokenService {
  private static final int TOKEN_BYTES = 32;
  private final boolean setupAvailable;
  private String token;

  private SetupTokenService(boolean adminExists, BootstrapAdminProperties bootstrapAdmin, Supplier<byte[]> tokenSource) {
    bootstrapAdmin.validate();
    setupAvailable = !adminExists && !bootstrapAdmin.configured();
    token = setupAvailable ? newToken(tokenSource) : null;
  }

  public static SetupTokenService create(
      boolean adminExists, BootstrapAdminProperties bootstrapAdmin, Supplier<byte[]> tokenSource) {
    return new SetupTokenService(adminExists, bootstrapAdmin, tokenSource);
  }

  public boolean setupAvailable() {
    return setupAvailable;
  }

  public String stdoutLine() {
    if (!setupAvailable) {
      return "";
    }
    return "mmpay-setup-token: " + token;
  }

  public boolean verify(String submittedToken) {
    if (!matches(submittedToken)) {
      return false;
    }
    token = null;
    return true;
  }

  public boolean matches(String submittedToken) {
    if (!setupAvailable || token == null || submittedToken == null) {
      return false;
    }
    var expected = token.getBytes(StandardCharsets.US_ASCII);
    var actual = submittedToken.getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(expected, actual);
  }

  private static String newToken(Supplier<byte[]> tokenSource) {
    var bytes = tokenSource.get();
    if (bytes.length != TOKEN_BYTES) {
      throw new IllegalArgumentException("setup token must be 32 bytes");
    }
    return HexFormat.of().formatHex(bytes);
  }
}
