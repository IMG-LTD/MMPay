package com.imgltd.mmpay.auditverifier;

import java.util.Map;

public record AuditRow(long id, String action, String prevRowHmac, String rowHmac, Map<String, String> details) {}
