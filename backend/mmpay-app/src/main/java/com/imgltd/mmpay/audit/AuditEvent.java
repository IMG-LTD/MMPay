package com.imgltd.mmpay.audit;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(
    long id,
    Instant timestamp,
    String actorKind,
    String actorId,
    String action,
    String targetKind,
    String targetId,
    Map<String, ?> details,
    String prevRowHmac,
    String rowHmac) {
  public AuditEvent withAction(String newAction) {
    return new AuditEvent(
        id, timestamp, actorKind, actorId, newAction, targetKind, targetId, details, prevRowHmac, rowHmac);
  }

  public AuditEvent withRowHmac(String newRowHmac) {
    return new AuditEvent(
        id, timestamp, actorKind, actorId, action, targetKind, targetId, details, prevRowHmac, newRowHmac);
  }
}
