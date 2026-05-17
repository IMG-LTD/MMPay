package com.imgltd.mmpay.webhook;

import java.util.Set;

public final class LicenseClaimFieldNames {
  private static final Set<String> PROHIBITED_FIELDS =
      Set.of("edition", "seats", "features", "issuedAt", "expiresAt");

  private LicenseClaimFieldNames() {}

  public static Set<String> prohibitedFields() {
    return PROHIBITED_FIELDS;
  }
}
