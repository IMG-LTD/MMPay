package com.imgltd.mmpay.merchant;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class InputValidator {
  private static final Pattern ID = Pattern.compile("[a-z0-9_-]{3,64}");
  private static final Pattern CREDENTIAL_REF = Pattern.compile("^env://[A-Z][A-Z0-9_]{2,127}$");
  private static final Set<String> RESERVED_PROVIDER_CODES = Set.of("none", "mock", "dummy");
  private static final int MAX_NAME = 128;

  private InputValidator() {}

  static void rejectTenant(Map<String, Object> raw) {
    if (raw.containsKey("tenant_id") || raw.containsKey("tenantId")) {
      throw AdminProblems.unprocessable(AdminProblems.TENANT_REJECTED, "tenant_id is server controlled");
    }
  }

  static void merchantCreate(MerchantCreateRequest request) {
    requireId(request.id());
    requireName(request.displayName());
    requireCredentialRef(request.credentialRef());
  }

  static void channelCreate(ChannelCreateRequest request) {
    requireId(request.id());
    requireName(request.displayName());
    requireCredentialRef(request.credentialRef());
    requireProviderCode(request.providerCode());
  }

  static void requireProviderCode(String code) {
    var value = requireText(code, "provider_code");
    if (RESERVED_PROVIDER_CODES.contains(value)) {
      throw AdminProblems.unprocessable(AdminProblems.PROVIDER_RESERVED, "provider code is reserved");
    }
  }

  private static void requireId(String value) {
    if (!ID.matcher(requireText(value, "id")).matches()) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "id invalid");
    }
  }

  private static void requireCredentialRef(String value) {
    if (!CREDENTIAL_REF.matcher(requireText(value, "credential_ref")).matches()) {
      throw AdminProblems.unprocessable(AdminProblems.CREDENTIAL_REF_INVALID, "credential_ref invalid");
    }
  }

  private static void requireName(String value) {
    var text = requireText(value, "display_name");
    if (text.length() > MAX_NAME) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, "display_name too long");
    }
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw AdminProblems.unprocessable(AdminProblems.MASS_ASSIGNMENT, field + " required");
    }
    return value;
  }
}
