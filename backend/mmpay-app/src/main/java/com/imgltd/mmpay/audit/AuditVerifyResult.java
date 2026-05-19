package com.imgltd.mmpay.audit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditVerifyResult(
    @JsonProperty("from_id") long fromId,
    @JsonProperty("to_id") long toId,
    boolean ok,
    @JsonProperty("first_break_id") Long firstBreakId) {}
