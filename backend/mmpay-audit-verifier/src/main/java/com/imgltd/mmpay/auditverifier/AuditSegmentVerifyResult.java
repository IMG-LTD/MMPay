package com.imgltd.mmpay.auditverifier;

import java.util.List;

public record AuditSegmentVerifyResult(boolean ok, int segmentCount, List<AuditSegmentBreak> segmentBreaks) {}
