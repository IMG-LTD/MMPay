package com.imgltd.mmpay.audit;

import java.util.List;

public interface AuditEventStore {
  AuditEvent append(AuditAppendRequest request);

  List<AuditEvent> events();
}
