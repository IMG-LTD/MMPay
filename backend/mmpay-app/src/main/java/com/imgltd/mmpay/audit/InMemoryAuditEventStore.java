package com.imgltd.mmpay.audit;

import java.util.List;

public final class InMemoryAuditEventStore implements AuditEventStore {
  private final AuditChain auditChain;

  public InMemoryAuditEventStore(AuditChain auditChain) {
    this.auditChain = auditChain;
  }

  @Override
  public AuditEvent append(AuditAppendRequest request) {
    return auditChain.emit(
        request.actorKind(),
        request.actorId(),
        request.action(),
        request.targetKind(),
        request.targetId(),
        request.details());
  }

  @Override
  public List<AuditEvent> events() {
    return auditChain.events();
  }
}
