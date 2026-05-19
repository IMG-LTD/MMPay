package com.imgltd.mmpay.auditverifier;

public record AuditSegmentBreak(long atRowId, boolean validated, String assertion) {}
