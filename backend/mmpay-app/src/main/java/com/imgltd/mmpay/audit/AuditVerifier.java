package com.imgltd.mmpay.audit;

import com.imgltd.mmpay.auditverifier.AuditRow;
import com.imgltd.mmpay.auditverifier.AuditSegmentBreak;
import com.imgltd.mmpay.auditverifier.AuditSegmentVerifier;
import com.imgltd.mmpay.auditverifier.AuditSegmentVerifierOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditVerifier {
  private final AuditEventStore eventStore;
  private final AuditHasher hasher;
  private final AuditSegmentVerifier segmentVerifier;

  public AuditVerifier(AuditEventStore eventStore, AuditHasher hasher) {
    this(eventStore, hasher, rejectingSegmentVerifier());
  }

  public AuditVerifier(AuditEventStore eventStore, AuditHasher hasher, AuditSegmentVerifier segmentVerifier) {
    this.eventStore = eventStore;
    this.hasher = hasher;
    this.segmentVerifier = segmentVerifier;
  }

  public AuditVerifyResult verify() {
    var events = eventStore.events();
    var segmentResult = segmentVerifier.verify(rows(events));
    var breakStatuses = segmentResult.segmentBreaks().stream().map(AuditVerifier::breakStatus).toList();
    if (!segmentResult.ok()) {
      return result(events, new VerifyOutcome(false, firstFailedBreak(segmentResult.segmentBreaks()), breakStatuses));
    }
    return verifyEventHmacs(events, acceptedBreakIds(segmentResult.segmentBreaks()), breakStatuses);
  }

  String keyFingerprint() {
    return hasher.keyFingerprint();
  }

  private AuditVerifyResult verifyEventHmacs(
      List<AuditEvent> events, Set<Long> acceptedBreakIds, List<AuditVerifyResult.AuditSegmentBreakStatus> breaks) {
    String expectedPrevious = null;
    for (var event : events) {
      var previousForHmac = acceptedBreakIds.contains(event.id()) ? event.prevRowHmac() : expectedPrevious;
      if (!isContinuable(event, expectedPrevious, acceptedBreakIds) || !validRowHmac(event, previousForHmac)) {
        return result(events, new VerifyOutcome(false, event.id(), breaks));
      }
      expectedPrevious = event.rowHmac();
    }
    return result(events, new VerifyOutcome(true, null, breaks));
  }

  private boolean validRowHmac(AuditEvent event, String previousForHmac) {
    return hasher.rowHmac(event, previousForHmac).equals(event.rowHmac());
  }

  private static AuditSegmentVerifier rejectingSegmentVerifier() {
    return new AuditSegmentVerifier(new AuditSegmentVerifierOptions(Set.of(), Set.of(), details -> false, (signature, payload) -> false));
  }

  private AuditVerifyResult result(List<AuditEvent> events, VerifyOutcome outcome) {
    var segments = buildSegments(events, acceptedBreakStatusIds(outcome.breaks()), hasher.keyFingerprint());
    return new AuditVerifyResult(
        firstId(events), lastId(events), outcome.ok(), outcome.firstBreakId(), segments.size(), segments, outcome.breaks());
  }

  private static List<AuditVerifyResult.AuditSegmentStatus> buildSegments(
      List<AuditEvent> events, Set<Long> acceptedBreakIds, String keyFingerprint) {
    if (events.isEmpty()) {
      return List.of();
    }
    var segments = new ArrayList<AuditVerifyResult.AuditSegmentStatus>();
    var window = SegmentWindow.first(events.getFirst().id(), keyFingerprint);
    for (int index = 0; index < events.size(); index++) {
      var event = events.get(index);
      if (acceptedBreakIds.contains(event.id()) && index > 0) {
        segments.add(window.close(events.get(index - 1).id()));
        window = SegmentWindow.fromBreak(segments.size(), event, keyFingerprint);
      }
    }
    segments.add(window.close(events.getLast().id()));
    return List.copyOf(segments);
  }

  private static boolean isContinuable(AuditEvent event, String expectedPrevious, Set<Long> acceptedBreakIds) {
    return acceptedBreakIds.contains(event.id()) || equalsNullable(expectedPrevious, event.prevRowHmac());
  }

  private static Set<Long> acceptedBreakIds(List<AuditSegmentBreak> breaks) {
    return breaks.stream().filter(AuditSegmentBreak::validated).map(AuditSegmentBreak::atRowId).collect(Collectors.toSet());
  }

  private static Set<Long> acceptedBreakStatusIds(List<AuditVerifyResult.AuditSegmentBreakStatus> breaks) {
    return breaks.stream()
        .filter(AuditVerifyResult.AuditSegmentBreakStatus::validated)
        .map(AuditVerifyResult.AuditSegmentBreakStatus::atRowId)
        .collect(Collectors.toSet());
  }

  private static AuditVerifyResult.AuditSegmentBreakStatus breakStatus(AuditSegmentBreak segmentBreak) {
    return new AuditVerifyResult.AuditSegmentBreakStatus(segmentBreak.atRowId(), segmentBreak.validated(), segmentBreak.assertion());
  }

  private static Long firstFailedBreak(List<AuditSegmentBreak> breaks) {
    return breaks.stream().filter(segmentBreak -> !segmentBreak.validated()).findFirst().map(AuditSegmentBreak::atRowId).orElse(null);
  }

  private static List<AuditRow> rows(List<AuditEvent> events) {
    return events.stream().map(AuditVerifier::row).toList();
  }

  private static AuditRow row(AuditEvent event) {
    return new AuditRow(event.id(), event.action(), event.prevRowHmac(), event.rowHmac(), stringDetails(event.details()));
  }

  private static Map<String, String> stringDetails(Map<String, ?> details) {
    return details.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
  }

  private static long firstId(List<AuditEvent> events) {
    return events.isEmpty() ? 0L : events.getFirst().id();
  }

  private static long lastId(List<AuditEvent> events) {
    return events.isEmpty() ? 0L : events.getLast().id();
  }

  private static boolean equalsNullable(String expected, String actual) {
    return expected == null ? actual == null : expected.equals(actual);
  }

  private record SegmentWindow(
      int segmentId, long firstId, String keyFingerprint, Long restoreRowId, String restoreNonce, Boolean pgpValid) {
    static SegmentWindow first(long firstId, String keyFingerprint) {
      return new SegmentWindow(0, firstId, keyFingerprint, null, null, null);
    }

    static SegmentWindow fromBreak(int segmentId, AuditEvent event, String keyFingerprint) {
      return new SegmentWindow(
          segmentId,
          event.id(),
          keyFingerprint,
          event.id(),
          String.valueOf(event.details().get("restore_nonce")),
          true);
    }

    AuditVerifyResult.AuditSegmentStatus close(long lastId) {
      return new AuditVerifyResult.AuditSegmentStatus(
          segmentId, firstId, lastId, keyFingerprint, true, restoreRowId, restoreNonce, pgpValid);
    }
  }

  private record VerifyOutcome(
      boolean ok, Long firstBreakId, List<AuditVerifyResult.AuditSegmentBreakStatus> breaks) {}
}
