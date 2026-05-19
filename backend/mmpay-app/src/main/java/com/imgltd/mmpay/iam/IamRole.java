package com.imgltd.mmpay.iam;

import java.util.Locale;
import java.util.Set;

final class IamRole {
  private static final Set<String> USER_ROLES = Set.of("admin", "ops", "finance", "auditor");

  private IamRole() {}

  static String requireUserRole(String role) {
    var normalized = role == null ? "" : role.toLowerCase(Locale.ROOT);
    if (!USER_ROLES.contains(normalized)) {
      throw new IllegalArgumentException("iam_role_invalid");
    }
    return normalized;
  }

  static String authority(String role) {
    return "ROLE_" + requireUserRole(role).toUpperCase(Locale.ROOT);
  }
}
