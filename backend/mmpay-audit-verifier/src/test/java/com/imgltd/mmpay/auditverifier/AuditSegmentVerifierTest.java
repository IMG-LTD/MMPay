package com.imgltd.mmpay.auditverifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuditSegmentVerifierTest {
  @Test
  void rejectsSegmentBreakWithoutRestoreAttestation() {
    var result = verifier().verify(rows(row(1, "system.start", null, "h1"), row(2, "system.restore.complete", "old", "h2")));

    assertEquals(false, result.ok());
    assertEquals("attestation_required", result.segmentBreaks().getFirst().assertion());
  }

  @Test
  void rejectsReplayedRestoreNonce() {
    var result = verifierWithConsumedNonce().verify(rows(row(1, "system.start", null, "h1"), attestedBreak("nonce-1")));

    assertEquals(false, result.ok());
    assertEquals("nonce_replay", result.segmentBreaks().getFirst().assertion());
  }

  @Test
  void rejectsMissingOperatorPgpSignature() {
    var details = attestationDetails("nonce-1");
    details.remove("attestation_pgp_sig");

    var result = verifier().verify(rows(row(1, "system.start", null, "h1"), row(2, "system.restore.complete", "old", "h2", details)));

    assertEquals(false, result.ok());
    assertEquals("operator_pgp_required", result.segmentBreaks().getFirst().assertion());
  }

  @Test
  void acceptsProperlyAttestedSegmentBreak() {
    var result = verifier().verify(rows(row(1, "system.start", null, "h1"), attestedBreak("nonce-1"), row(3, "system.start", "h2", "h3")));

    assertTrue(result.ok());
    assertEquals(2, result.segmentCount());
    assertEquals(true, result.segmentBreaks().getFirst().validated());
  }

  private static AuditSegmentVerifier verifier() {
    return new AuditSegmentVerifier(Set.of("nonce-1"), Set.of(), signature -> "valid-signature".equals(signature));
  }

  private static AuditSegmentVerifier verifierWithConsumedNonce() {
    return new AuditSegmentVerifier(Set.of("nonce-1"), Set.of("nonce-1"), signature -> true);
  }

  private static List<AuditRow> rows(AuditRow... rows) {
    return List.of(rows);
  }

  private static AuditRow attestedBreak(String nonce) {
    return row(2, "system.restore.complete", "old", "h2", attestationDetails(nonce));
  }

  private static Map<String, String> attestationDetails(String nonce) {
    return new java.util.LinkedHashMap<>(
        Map.of(
            "prev_segment_terminal_row_hmac", "old",
            "backup_file_sha256", "sha256:backup",
            "snapshot_timestamp", "2026-05-19T00:00:00Z",
            "restore_nonce", nonce,
            "attestation_hmac", "hmac",
            "attestation_pgp_sig", "valid-signature"));
  }

  private static AuditRow row(long id, String action, String prev, String hmac) {
    return row(id, action, prev, hmac, Map.of());
  }

  private static AuditRow row(long id, String action, String prev, String hmac, Map<String, String> details) {
    return new AuditRow(id, action, prev, hmac, details);
  }
}
