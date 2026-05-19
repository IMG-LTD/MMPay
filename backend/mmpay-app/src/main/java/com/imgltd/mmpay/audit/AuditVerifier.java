package com.imgltd.mmpay.audit;

public final class AuditVerifier {
  private final AuditEventStore eventStore;
  private final AuditHasher hasher;

  public AuditVerifier(AuditEventStore eventStore, AuditHasher hasher) {
    this.eventStore = eventStore;
    this.hasher = hasher;
  }

  public AuditVerifyResult verify() {
    return hasher.verify(eventStore.events());
  }
}
