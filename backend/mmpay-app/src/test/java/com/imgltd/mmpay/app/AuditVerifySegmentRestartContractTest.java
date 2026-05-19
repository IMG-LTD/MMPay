package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgltd.mmpay.audit.AuditEvent;
import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.audit.AuditHasher;
import com.imgltd.mmpay.audit.AuditVerifier;
import com.imgltd.mmpay.auditverifier.AuditSegmentVerifier;
import com.imgltd.mmpay.auditverifier.AuditSegmentVerifierOptions;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AuditVerifySegmentRestartContractTest {
  private static final byte[] AUDIT_KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

  @Test
  void auditVerifyAcceptsProperlyAttestedSegmentBreak() {
    var events = attestedSegmentEvents();
    var result = verifier(events, Set.of("nonce-1"), Set.of()).verify();

    assertThat(result.ok()).isTrue();
    assertThat(result.segmentCount()).isEqualTo(2);
    assertThat(result.segmentBreaks()).extracting("validated").containsExactly(true);
    assertThat(result.segmentBreaks()).extracting("assertion").containsExactly("accepted");
  }

  @Test
  void restoreReseatForgeryWithoutAttestationIsRejected() {
    var result = verifier(unattestedSegmentEvents(), Set.of("nonce-1"), Set.of()).verify();

    assertThat(result.ok()).isFalse();
    assertThat(result.firstBreakId()).isEqualTo(2L);
    assertThat(result.segmentBreaks()).extracting("assertion").containsExactly("attestation_required");
  }

  @Test
  void consumedRestoreNonceIsRejectedByAuditVerify() {
    var events = attestedSegmentEvents();
    var result = verifier(events, Set.of("nonce-1"), Set.of("nonce-1")).verify();

    assertThat(result.ok()).isFalse();
    assertThat(result.firstBreakId()).isEqualTo(2L);
    assertThat(result.segmentBreaks()).extracting("assertion").containsExactly("nonce_replay");
  }

  private static AuditVerifier verifier(List<AuditEvent> events, Set<String> knownNonces, Set<String> consumedNonces) {
    return new AuditVerifier(new StaticAuditEventStore(events), hasher(), segmentVerifier(knownNonces, consumedNonces));
  }

  private static AuditSegmentVerifier segmentVerifier(Set<String> knownNonces, Set<String> consumedNonces) {
    return new AuditSegmentVerifier(
        new AuditSegmentVerifierOptions(knownNonces, consumedNonces, details -> true, (signature, payload) -> "valid-signature".equals(signature)));
  }

  private static List<AuditEvent> attestedSegmentEvents() {
    var first = signed(event(1, "system.start", null, Map.of()), null);
    var segmentBreak = signed(event(2, "system.restore.complete", "legacy-terminal", attestationDetails()), "legacy-terminal");
    var next = signed(event(3, "system.start", segmentBreak.rowHmac(), Map.of()), segmentBreak.rowHmac());
    return List.of(first, segmentBreak, next);
  }

  private static List<AuditEvent> unattestedSegmentEvents() {
    var first = signed(event(1, "system.start", null, Map.of()), null);
    var segmentBreak = signed(event(2, "system.restore.complete", "legacy-terminal", Map.of()), "legacy-terminal");
    return List.of(first, segmentBreak);
  }

  private static Map<String, ?> attestationDetails() {
    return Map.of(
        "prev_segment_terminal_row_hmac", "legacy-terminal",
        "backup_file_sha256", "sha256:backup",
        "snapshot_timestamp", "2026-05-19T00:00:00Z",
        "restore_nonce", "nonce-1",
        "attestation_hmac", "hmac",
        "attestation_pgp_sig", "valid-signature");
  }

  private static AuditEvent signed(AuditEvent event, String previous) {
    return event.withRowHmac(hasher().rowHmac(event, previous));
  }

  private static AuditEvent event(long id, String action, String previous, Map<String, ?> details) {
    return new AuditEvent(id, Instant.parse("2026-05-19T00:00:00Z"), "system", null, action, "runtime", "mmpay", details, previous, "");
  }

  private static AuditHasher hasher() {
    return new AuditHasher(AUDIT_KEY);
  }

  private record StaticAuditEventStore(List<AuditEvent> events) implements AuditEventStore {
    @Override
    public AuditEvent append(com.imgltd.mmpay.audit.AuditAppendRequest request) {
      throw new UnsupportedOperationException("test store is read-only");
    }
  }
}
