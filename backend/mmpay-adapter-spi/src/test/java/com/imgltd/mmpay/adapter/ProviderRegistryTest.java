package com.imgltd.mmpay.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ProviderRegistryTest {
  @Test
  void indexesProviderDescriptorsByCode() {
    var descriptor = new TestProviderDescriptor("huifu", "Huifu", Set.of("HUIFU_MERCHANT_ID"));
    var registry = new ProviderRegistry(Set.of(descriptor));

    assertEquals(descriptor, registry.require("huifu"));
  }

  @Test
  void rejectsReservedProviderCodesAtInitialization() {
    var descriptor = new TestProviderDescriptor("mock", "Mock", Set.of());

    assertThrows(IllegalArgumentException.class, () -> new ProviderRegistry(Set.of(descriptor)));
  }

  @Test
  void unknownProviderCodeFailsExplicitly() {
    var registry = new ProviderRegistry(Set.of(new TestProviderDescriptor("huifu", "Huifu", Set.of())));

    assertThrows(IllegalArgumentException.class, () -> registry.require("none"));
  }

  private record TestProviderDescriptor(String code, String displayName, Set<String> requiredEnvKeys)
      implements ProviderDescriptor {}
}
