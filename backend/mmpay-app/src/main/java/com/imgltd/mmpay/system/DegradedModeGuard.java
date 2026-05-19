package com.imgltd.mmpay.system;

import com.imgltd.mmpay.merchant.AdminProblemException;
import org.springframework.http.HttpStatus;

public final class DegradedModeGuard {
  public static final String DEGRADED_MODE_BLOCKED = "urn:mmpay:problem:degraded-mode-blocked";
  private final boolean degraded;

  public DegradedModeGuard(boolean degraded) {
    this.degraded = degraded;
  }

  public boolean isDegraded() {
    return degraded;
  }

  public void requireWriteAllowed(String surface) {
    if (degraded) {
      throw blockedProblem(surface);
    }
  }

  public void requireAuditVerifyAllowed() {
    // Audit verify is explicitly allowed in degraded mode because it is read-only.
  }

  public AdminProblemException blockedProblem(String surface) {
    return new AdminProblemException(
        DEGRADED_MODE_BLOCKED, HttpStatus.SERVICE_UNAVAILABLE, "degraded-mode-blocked: " + surface);
  }
}
