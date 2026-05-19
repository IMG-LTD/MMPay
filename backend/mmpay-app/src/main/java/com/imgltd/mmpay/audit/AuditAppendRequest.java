package com.imgltd.mmpay.audit;

import java.util.Map;

public record AuditAppendRequest(
    String actorKind, String actorId, String action, String targetKind, String targetId, Map<String, ?> details) {}
