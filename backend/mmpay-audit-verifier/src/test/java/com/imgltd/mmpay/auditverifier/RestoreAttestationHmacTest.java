package com.imgltd.mmpay.auditverifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RestoreAttestationHmacTest {
  private static final byte[] ATTESTATION_KEY =
      "restore-attestation-key-32-bytes!".getBytes(StandardCharsets.UTF_8);

  @Test
  void rejectsInvalidRestoreAttestationHmac() {
    var details = attestationDetails();
    details.put("attestation_hmac", "invalid");

    var result = verifier().verify(List.of(row(1, "system.start", null, "h1"), row(2, "system.restore.complete", "old", "h2", details)));

    assertEquals(false, result.ok());
    assertEquals("attestation_hmac_invalid", result.segmentBreaks().getFirst().assertion());
  }

  @Test
  void acceptsValidRestoreAttestationHmacAndOperatorPgpSignature() {
    var result = verifier().verify(List.of(row(1, "system.start", null, "h1"), attestedBreak()));

    assertEquals(true, result.ok());
    assertEquals("accepted", result.segmentBreaks().getFirst().assertion());
  }

  private static AuditSegmentVerifier verifier() {
    return new AuditSegmentVerifier(
        new AuditSegmentVerifierOptions(
            Set.of("nonce-1"),
            Set.of(),
            new RestoreAttestationHmacVerifier(ATTESTATION_KEY),
            (signature, payload) -> "valid-signature".equals(signature) && payload.contains("nonce-1")));
  }

  private static AuditRow attestedBreak() {
    return row(2, "system.restore.complete", "old", "h2", attestationDetails());
  }

  private static Map<String, String> attestationDetails() {
    var details =
        new java.util.LinkedHashMap<>(
            Map.of(
                "prev_segment_terminal_row_hmac", "old",
                "backup_file_sha256", "sha256:backup",
                "snapshot_timestamp", "2026-05-19T00:00:00Z",
                "restore_nonce", "nonce-1",
                "attestation_pgp_sig", "valid-signature"));
    details.put("attestation_hmac", RestoreAttestationHmacVerifier.computeHex(ATTESTATION_KEY, details));
    return details;
  }

  private static AuditRow row(long id, String action, String prev, String hmac) {
    return row(id, action, prev, hmac, Map.of());
  }

  private static AuditRow row(long id, String action, String prev, String hmac, Map<String, String> details) {
    return new AuditRow(id, action, prev, hmac, details);
  }
}
