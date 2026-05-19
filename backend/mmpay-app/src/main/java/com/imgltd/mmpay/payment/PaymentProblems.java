package com.imgltd.mmpay.payment;

import com.imgltd.mmpay.merchant.AdminProblemException;
import org.springframework.http.HttpStatus;

final class PaymentProblems {
  static final String BULK_TOO_LARGE = "urn:mmpay:problem:bulk-redispatch-too-large";
  static final String FINGERPRINT_REBIND_REQUIRED = "urn:mmpay:problem:fingerprint-rebind-required";
  static final String PROVIDER_CALLBACK_SIGNATURE_INVALID =
      "urn:mmpay:problem:provider-callback-signature-invalid";
  static final String PROVIDER_LIVE_DISABLED = "urn:mmpay:problem:provider-live-disabled";
  static final String REFUND_EXCEEDS_INTENT = "urn:mmpay:problem:refund-exceeds-intent";
  static final String REFUND_ON_NON_SUCCEEDED = "urn:mmpay:problem:refund-on-non-succeeded";
  static final String STATE_TRANSITION_ILLEGAL = "urn:mmpay:problem:state-transition-illegal";
  static final String TARGET_URL_REJECTED = "urn:mmpay:problem:target-url-rejected";

  private PaymentProblems() {}

  static AdminProblemException conflict(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.CONFLICT, detail);
  }

  static AdminProblemException serviceUnavailable(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.SERVICE_UNAVAILABLE, detail);
  }

  static AdminProblemException unauthorized(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.UNAUTHORIZED, detail);
  }

  static AdminProblemException unprocessable(String type, String detail) {
    return new AdminProblemException(type, HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }
}
