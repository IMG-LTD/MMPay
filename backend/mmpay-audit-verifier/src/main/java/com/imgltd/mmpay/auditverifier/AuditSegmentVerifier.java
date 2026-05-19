package com.imgltd.mmpay.auditverifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AuditSegmentVerifier {
  private static final String RESTORE_COMPLETE = "system.restore.complete";
  private static final List<String> REQUIRED_ATTESTATION_KEYS =
      List.of(
          "prev_segment_terminal_row_hmac",
          "backup_file_sha256",
          "snapshot_timestamp",
          "restore_nonce",
          "attestation_hmac");

  private final Set<String> knownNonces;
  private final Set<String> consumedNonces;
  private final RestoreAttestationVerifier restoreAttestationVerifier;
  private final OperatorPgpVerifier pgpVerifier;

  public AuditSegmentVerifier(AuditSegmentVerifierOptions options) {
    this.knownNonces = options.knownNonces();
    this.consumedNonces = options.consumedNonces();
    this.restoreAttestationVerifier = options.restoreAttestationVerifier();
    this.pgpVerifier = options.operatorPgpVerifier();
  }

  public AuditSegmentVerifyResult verify(List<AuditRow> rows) {
    var breaks = new ArrayList<AuditSegmentBreak>();
    String expectedPrevious = null;
    int segmentCount = rows.isEmpty() ? 0 : 1;
    for (AuditRow row : rows) {
      if (!equalsNullable(expectedPrevious, row.prevRowHmac())) {
        var assertion = validateSegmentBreak(row);
        if (assertion != null) {
          breaks.add(new AuditSegmentBreak(row.id(), false, assertion));
          return new AuditSegmentVerifyResult(false, segmentCount, List.copyOf(breaks));
        }
        segmentCount++;
        breaks.add(new AuditSegmentBreak(row.id(), true, "accepted"));
      }
      expectedPrevious = row.rowHmac();
    }
    return new AuditSegmentVerifyResult(true, segmentCount, List.copyOf(breaks));
  }

  private String validateSegmentBreak(AuditRow row) {
    if (!RESTORE_COMPLETE.equals(row.action()) || missingRequiredAttestation(row.details())) {
      return "attestation_required";
    }
    if (!row.prevRowHmac().equals(row.details().get("prev_segment_terminal_row_hmac"))) {
      return "prev_segment_mismatch";
    }
    var nonce = row.details().get("restore_nonce");
    if (!knownNonces.contains(nonce)) {
      return "restore_nonce_missing";
    }
    if (consumedNonces.contains(nonce)) {
      return "nonce_replay";
    }
    if (!restoreAttestationVerifier.verify(row.details())) {
      return "attestation_hmac_invalid";
    }
    return verifyOperatorSignature(row.details());
  }

  private String verifyOperatorSignature(Map<String, String> details) {
    var signature = details.get("attestation_pgp_sig");
    if (signature == null || signature.isBlank()) {
      return "operator_pgp_required";
    }
    var payload = RestoreAttestationHmacVerifier.payload(details);
    return pgpVerifier.verify(signature, payload) ? null : "operator_pgp_invalid";
  }

  private static boolean missingRequiredAttestation(Map<String, String> details) {
    return REQUIRED_ATTESTATION_KEYS.stream().anyMatch(key -> !details.containsKey(key));
  }

  private static boolean equalsNullable(String expected, String actual) {
    return expected == null ? actual == null : expected.equals(actual);
  }
}
