package com.imgltd.mmpay.app.evidence;

import com.imgltd.mmpay.app.integrations.IntegrationService;
import com.imgltd.mmpay.merchant.AdminProblemException;
import com.imgltd.mmpay.system.DegradedModeGuard;
import java.time.Clock;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceSnapshotController {
  private final IntegrationService integrationService;
  private final Clock clock;
  private final DegradedModeGuard degradedModeGuard;

  public EvidenceSnapshotController(
      IntegrationService integrationService, Clock clock, DegradedModeGuard degradedModeGuard) {
    this.integrationService = integrationService;
    this.clock = clock;
    this.degradedModeGuard = degradedModeGuard;
  }

  @PostMapping("/api/admin/evidence/snapshot")
  @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
  Map<String, Object> snapshot() {
    degradedModeGuard.requireWriteAllowed("evidence-snapshot");
    long forwarded = integrationService.successfulRelayForwardCount();
    if (forwarded == 0) {
      throw new AdminProblemException(
          "urn:mmpay:problem:evidence-precheck-failed", HttpStatus.PRECONDITION_FAILED, "relay evidence is missing");
    }
    return Map.of("captured_at", clock.instant().toString(), "license_relay_forwarded", forwarded);
  }
}
