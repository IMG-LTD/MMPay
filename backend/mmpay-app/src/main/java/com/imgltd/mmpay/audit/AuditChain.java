package com.imgltd.mmpay.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AuditChain {
  private final AuditHasher hasher;
  private final Clock clock;
  private final List<AuditEvent> events = new ArrayList<>();

  public AuditChain(byte[] hmacKey) {
    this(new AuditHasher(hmacKey), Clock.systemUTC());
  }

  public AuditChain(byte[] hmacKey, Clock clock) {
    this(new AuditHasher(hmacKey), clock);
  }

  public AuditChain(AuditHasher hasher, Clock clock) {
    this.hasher = hasher;
    this.clock = clock;
  }

  public synchronized AuditEvent emit(
      String actorKind, String actorId, String action, String targetKind, String targetId, Map<String, ?> details) {
    AuditDetails.validate(details);
    var previous = events.isEmpty() ? null : events.getLast().rowHmac();
    var id = events.size() + 1L;
    var timestamp = Instant.now(clock);
    var draft =
        new AuditEvent(id, timestamp, actorKind, actorId, action, targetKind, targetId, Map.copyOf(details), previous, "");
    var event = draft.withRowHmac(hasher.rowHmac(draft, previous));
    events.add(event);
    return event;
  }

  public synchronized List<AuditEvent> events() {
    return new ArrayList<>(events);
  }

  public AuditVerifyResult verify() {
    return verify(events());
  }

  public AuditVerifyResult verify(List<AuditEvent> candidateEvents) {
    return hasher.verify(candidateEvents);
  }
}
