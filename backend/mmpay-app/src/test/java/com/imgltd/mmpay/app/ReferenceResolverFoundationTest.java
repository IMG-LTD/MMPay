package com.imgltd.mmpay.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import com.imgltd.mmpay.credentials.ReferenceResolutionException;
import com.imgltd.mmpay.credentials.ReferenceResolutionFailure;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReferenceResolverFoundationTest {
  @Test
  void resolvesEnvReferencesAndComputesEightHexFingerprint() {
    var resolver = new EnvironmentReferenceResolver(Map.of("MMPAY_HUIFU_KEY", "material"));

    var resolved = resolver.resolve("env://MMPAY_HUIFU_KEY");

    assertEquals("env://MMPAY_HUIFU_KEY", resolved.uri());
    assertEquals("40b30b4e", resolved.fingerprint8());
  }

  @Test
  void rejectsInvalidEmptyOversizeMissingAndKmsReferencesExplicitly() {
    var oversize = "x".repeat(64 * 1024 + 1);
    var resolver = new EnvironmentReferenceResolver(Map.of("EMPTY", " ", "OVERSIZE", oversize));

    assertFailure(resolver, "env://bad", ReferenceResolutionFailure.SCHEME_INVALID);
    assertFailure(resolver, "env://EMPTY", ReferenceResolutionFailure.EMPTY_BYTES);
    assertFailure(resolver, "env://OVERSIZE", ReferenceResolutionFailure.VALUE_TOO_LARGE);
    assertFailure(resolver, "env://MISSING_KEY", ReferenceResolutionFailure.ENV_UNSET);
    assertFailure(resolver, "kms://vault/path", ReferenceResolutionFailure.KMS_NO_RESOLVER);
  }

  private static void assertFailure(
      EnvironmentReferenceResolver resolver, String uri, ReferenceResolutionFailure failure) {
    var exception = assertThrows(ReferenceResolutionException.class, () -> resolver.resolve(uri));
    assertEquals(failure, exception.failure());
  }
}
