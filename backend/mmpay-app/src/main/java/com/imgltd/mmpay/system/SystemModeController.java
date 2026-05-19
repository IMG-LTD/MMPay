package com.imgltd.mmpay.system;

import com.imgltd.mmpay.audit.AuditWriter;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemModeController {
  private final AuditWriter auditWriter;

  public SystemModeController(AuditWriter auditWriter) {
    this.auditWriter = auditWriter;
  }

  @PostMapping("/api/admin/system/clear-degraded")
  @PreAuthorize("hasRole('ADMIN')")
  Map<String, Object> clearDegraded(Authentication authentication) {
    auditWriter.emit(
        "user", actor(authentication), "system.degraded_mode_exited", "runtime", "mmpay", Map.of("result", "accepted"));
    return Map.of("cleared", true);
  }

  private static String actor(Authentication authentication) {
    return authentication == null || authentication.getName() == null ? "unknown" : authentication.getName();
  }
}
