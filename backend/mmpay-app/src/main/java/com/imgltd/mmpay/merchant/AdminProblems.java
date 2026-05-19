package com.imgltd.mmpay.merchant;

import org.springframework.http.HttpStatus;

final class AdminProblems {
  static final String ACTIVE_ID_COLLISION = "urn:mmpay:problem:archived-id-reuse-collision";
  static final String ARCHIVED_TARGET = "urn:mmpay:problem:archived-target";
  static final String CREDENTIAL_REF_INVALID = "urn:mmpay:problem:credential-ref-invalid";
  static final String FK_ARCHIVED_MERCHANT = "urn:mmpay:problem:fk-archived-merchant";
  static final String IDEMPOTENCY_MISMATCH = "urn:mmpay:problem:idempotency-key-replay-mismatch";
  static final String MASS_ASSIGNMENT = "urn:mmpay:problem:patch-mass-assignment-rejected";
  static final String PROVIDER_RESERVED = "urn:mmpay:problem:provider-code-reserved";
  static final String PROVIDER_UNKNOWN = "urn:mmpay:problem:provider-code-unknown";
  static final String TENANT_REJECTED = "urn:mmpay:problem:tenant-id-rejected";

  private AdminProblems() {}

  static AdminProblemException conflict(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.CONFLICT, detail);
  }

  static AdminProblemException unprocessable(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }
}
