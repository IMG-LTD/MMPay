package com.imgltd.mmpay.audit;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditAdminController {
  private final AuditEventStore eventStore;
  private final AuditVerifier auditVerifier;

  public AuditAdminController(AuditEventStore eventStore, AuditVerifier auditVerifier) {
    this.eventStore = eventStore;
    this.auditVerifier = auditVerifier;
  }

  @GetMapping("/api/admin/audit")
  @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
  public AuditEventsResponse listAuditEvents() {
    return new AuditEventsResponse(eventStore.events());
  }

  @GetMapping("/api/admin/audit/verify")
  @PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
  public AuditVerifyResult verifyAuditChain() {
    return auditVerifier.verify();
  }

  public record AuditEventsResponse(List<AuditEvent> events) {}
}
