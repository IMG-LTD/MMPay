package com.imgltd.mmpay.audit;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record AuditVerifyResult(
    @JsonProperty("from_id") long fromId,
    @JsonProperty("to_id") long toId,
    boolean ok,
    @JsonProperty("first_break_id") Long firstBreakId,
    @JsonProperty("segment_count") int segmentCount,
    List<AuditSegmentStatus> segments,
    @JsonProperty("segment_breaks") List<AuditSegmentBreakStatus> segmentBreaks) {
  public AuditVerifyResult(long fromId, long toId, boolean ok, Long firstBreakId) {
    this(fromId, toId, ok, firstBreakId, defaultSegmentCount(fromId, toId), List.of(), List.of());
  }

  private static int defaultSegmentCount(long fromId, long toId) {
    return fromId == 0L && toId == 0L ? 0 : 1;
  }

  public record AuditSegmentStatus(
      @JsonProperty("segment_id") int segmentId,
      @JsonProperty("first_id") long firstId,
      @JsonProperty("last_id") long lastId,
      @JsonProperty("key_fingerprint") String keyFingerprint,
      boolean ok,
      @JsonProperty("restore_complete_row_id") Long restoreCompleteRowId,
      @JsonProperty("restore_nonce") String restoreNonce,
      @JsonProperty("attestation_pgp_sig_valid") Boolean attestationPgpSigValid) {}

  public record AuditSegmentBreakStatus(
      @JsonProperty("at_row_id") long atRowId, boolean validated, String assertion) {}
}
