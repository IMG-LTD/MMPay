package com.imgltd.mmpay.payment;

final class PaymentInput {
  private PaymentInput() {}

  static void requireAmount(long amountMinor) {
    if (amountMinor <= 0) {
      throw PaymentProblems.unprocessable("urn:mmpay:problem:amount-invalid", "amount_minor must be positive");
    }
  }

  static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw PaymentProblems.unprocessable("urn:mmpay:problem:request-invalid", name + " must not be blank");
    }
    return value;
  }
}
