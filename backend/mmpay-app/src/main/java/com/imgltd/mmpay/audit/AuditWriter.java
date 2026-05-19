package com.imgltd.mmpay.audit;

import java.util.Map;

public final class AuditWriter {
  private final AuditEventStore eventStore;

  public AuditWriter(AuditEventStore eventStore) {
    this.eventStore = eventStore;
  }

  public AuditEvent emit(
      String actorKind, String actorId, String action, String targetKind, String targetId, Map<String, ?> details) {
    return eventStore.append(new AuditAppendRequest(actorKind, actorId, action, targetKind, targetId, details));
  }
}
