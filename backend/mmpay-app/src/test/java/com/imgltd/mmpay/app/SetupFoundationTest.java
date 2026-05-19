package com.imgltd.mmpay.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import com.imgltd.mmpay.setup.MmpayActuatorSanitizer;
import com.imgltd.mmpay.setup.SetupTokenService;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class SetupFoundationTest {
  @Test
  void printsAndConsumesOneTimeSetupTokenOnlyWhenNoBootstrapEnvIsConfigured() {
    var tokenService = SetupTokenService.create(false, BootstrapAdminProperties.empty(), this::fixedTokenBytes);

    assertTrue(tokenService.stdoutLine().startsWith("mmpay-setup-token: "));
    assertFalse(tokenService.verify("wrong"));
    assertTrue(tokenService.verify(HexFormat.of().formatHex(fixedTokenBytes())));
    assertFalse(tokenService.verify(HexFormat.of().formatHex(fixedTokenBytes())));
  }

  @Test
  void setupRouteIsUnavailableWhenAdminExistsOrBootstrapEnvTakesPrecedence() {
    assertFalse(SetupTokenService.create(true, BootstrapAdminProperties.empty(), this::fixedTokenBytes).setupAvailable());
    assertFalse(
        SetupTokenService.create(
                false, new BootstrapAdminProperties("admin", "$2a$10$" + "a".repeat(53)), this::fixedTokenBytes)
            .setupAvailable());
  }

  @Test
  void sanitizerRedactsBootstrapAuditOauthAndCredentialNames() {
    var sanitizer = new MmpayActuatorSanitizer();

    assertEquals("******", sanitizer.sanitize("MMPAY_BOOTSTRAP_ADMIN_PASSWORD_HASH", "hash"));
    assertEquals("******", sanitizer.sanitize("MMPAY_AUDIT_HMAC_KEY", "key"));
    assertEquals("******", sanitizer.sanitize("MMPAY_OAUTH_CLIENT_SECRET", "secret"));
    assertEquals("plain", sanitizer.sanitize("MMPAY_PUBLIC_BASE_URL", "plain"));
  }

  @Test
  void invalidBootstrapPasswordHashFailsClosed() {
    var properties = new BootstrapAdminProperties("admin", "plain-password");

    assertThrows(IllegalArgumentException.class, properties::validate);
  }

  private byte[] fixedTokenBytes() {
    return "0123456789abcdef0123456789abcdef".getBytes();
  }
}
