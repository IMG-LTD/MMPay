package com.imgltd.mmpay.system;

import java.util.Map;

public final class RestoreModeGuard {
  private static final int CONFIGURATION_EXIT_CODE = 78;
  private static final String RESTORE_MODE = "MMPAY_RESTORE_MODE";
  private static final String RESTORE_ATTESTATION_KEY = "MMPAY_AUDIT_RESTORE_ATTESTATION_KEY";

  private RestoreModeGuard() {}

  public static void validate(Map<String, String> env) {
    boolean restoreMode = "true".equalsIgnoreCase(env.getOrDefault(RESTORE_MODE, "false"));
    boolean hasAttestationKey = hasText(env.get(RESTORE_ATTESTATION_KEY));
    if (!restoreMode && hasAttestationKey) {
      throw failed("restore-key-leaked-into-steady-state");
    }
    if (restoreMode && !hasAttestationKey) {
      throw failed("restore-mode-misconfig");
    }
  }

  private static RestoreModeConfigurationException failed(String message) {
    return new RestoreModeConfigurationException(message, CONFIGURATION_EXIT_CODE);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
