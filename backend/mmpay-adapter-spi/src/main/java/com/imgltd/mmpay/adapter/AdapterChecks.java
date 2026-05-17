package com.imgltd.mmpay.adapter;

final class AdapterChecks {
  private static final long MIN_AMOUNT_MINOR = 1L;

  private AdapterChecks() {}

  static void requirePositiveAmount(long amountMinor) {
    if (amountMinor < MIN_AMOUNT_MINOR) {
      throw new IllegalArgumentException("amountMinor must be positive");
    }
  }

  static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
