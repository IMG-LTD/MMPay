package com.imgltd.mmpay.auditverifier;

import java.util.Set;

public record AuditSegmentVerifierOptions(
    Set<String> knownNonces,
    Set<String> consumedNonces,
    RestoreAttestationVerifier restoreAttestationVerifier,
    OperatorPgpVerifier operatorPgpVerifier) {
  public AuditSegmentVerifierOptions {
    knownNonces = Set.copyOf(knownNonces);
    consumedNonces = Set.copyOf(consumedNonces);
  }
}
