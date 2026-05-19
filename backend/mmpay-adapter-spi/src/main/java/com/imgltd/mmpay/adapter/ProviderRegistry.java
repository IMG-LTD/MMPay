package com.imgltd.mmpay.adapter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ProviderRegistry {
  private static final Set<String> RESERVED_CODES = Set.of("none", "mock", "dummy");

  private final Map<String, ProviderDescriptor> descriptorsByCode;

  public ProviderRegistry(Set<ProviderDescriptor> descriptors) {
    Objects.requireNonNull(descriptors, "descriptors");
    descriptorsByCode = Map.copyOf(index(descriptors));
  }

  public ProviderDescriptor require(String code) {
    var normalizedCode = requireCode(code);
    var descriptor = descriptorsByCode.get(normalizedCode);
    if (descriptor == null) {
      throw new IllegalArgumentException("unknown provider code: " + normalizedCode);
    }
    return descriptor;
  }

  public Map<String, ProviderDescriptor> descriptors() {
    return descriptorsByCode;
  }

  private static Map<String, ProviderDescriptor> index(Set<ProviderDescriptor> descriptors) {
    var indexed = new LinkedHashMap<String, ProviderDescriptor>();
    for (var descriptor : descriptors) {
      var code = requireCode(descriptor.code());
      rejectReserved(code);
      if (indexed.putIfAbsent(code, descriptor) != null) {
        throw new IllegalArgumentException("duplicate provider code: " + code);
      }
    }
    return indexed;
  }

  private static String requireCode(String code) {
    var normalizedCode = AdapterChecks.requireText(code, "providerCode").trim();
    if (!normalizedCode.equals(normalizedCode.toLowerCase())) {
      throw new IllegalArgumentException("providerCode must be lowercase");
    }
    return normalizedCode;
  }

  private static void rejectReserved(String code) {
    if (RESERVED_CODES.contains(code)) {
      throw new IllegalArgumentException("providerCode is reserved: " + code);
    }
  }
}
