package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imgltd.mmpay.system.DegradedModeGuard;
import com.imgltd.mmpay.system.RestoreModeGuard;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class P5ReleaseClosureContractTest {
  @Test
  void degradedModeBlocksWritesButAllowsAuditVerify() {
    var guard = new DegradedModeGuard(true);

    assertThatThrownBy(() -> guard.requireWriteAllowed("refund"))
        .hasMessageContaining("degraded-mode-blocked");
    assertThatThrownBy(() -> guard.requireWriteAllowed("bulk-redispatch"))
        .hasMessageContaining("degraded-mode-blocked");
    assertThatThrownBy(() -> guard.requireWriteAllowed("evidence-snapshot"))
        .hasMessageContaining("degraded-mode-blocked");
    assertThatThrownBy(() -> guard.requireWriteAllowed("license-relay-forward"))
        .hasMessageContaining("degraded-mode-blocked");

    guard.requireAuditVerifyAllowed();
  }

  @Test
  void degradedModeIsNoopWhenDisabled() {
    var guard = new DegradedModeGuard(false);

    guard.requireWriteAllowed("refund");

    assertThat(guard.isDegraded()).isFalse();
  }

  @Test
  void restoreModeRejectsAttestationKeyInSteadyState() {
    var env = Map.of("MMPAY_AUDIT_RESTORE_ATTESTATION_KEY", "present");

    assertThatThrownBy(() -> RestoreModeGuard.validate(env))
        .hasMessageContaining("restore-key-leaked-into-steady-state")
        .extracting("exitCode")
        .isEqualTo(78);
  }

  @Test
  void restoreModeRequiresAttestationKey() {
    var env = Map.of("MMPAY_RESTORE_MODE", "true");

    assertThatThrownBy(() -> RestoreModeGuard.validate(env))
        .hasMessageContaining("restore-mode-misconfig")
        .extracting("exitCode")
        .isEqualTo(78);
  }

  @Test
  void degradedProblemUsesStableProblemDetails() {
    var problem = new DegradedModeGuard(true).blockedProblem("license-relay-forward");

    assertThat(problem.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(problem.type()).isEqualTo("urn:mmpay:problem:degraded-mode-blocked");
  }
}
