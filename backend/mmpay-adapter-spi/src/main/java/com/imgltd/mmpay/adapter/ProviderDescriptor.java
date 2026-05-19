package com.imgltd.mmpay.adapter;

import java.util.Set;

public interface ProviderDescriptor {
  String code();

  String displayName();

  Set<String> requiredEnvKeys();
}
