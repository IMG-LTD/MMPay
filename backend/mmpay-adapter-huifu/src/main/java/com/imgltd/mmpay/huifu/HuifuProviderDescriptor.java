package com.imgltd.mmpay.huifu;

import com.imgltd.mmpay.adapter.ProviderDescriptor;
import java.util.Set;

public final class HuifuProviderDescriptor implements ProviderDescriptor {
  private static final String CODE = "huifu";
  private static final String DISPLAY_NAME = "Huifu";
  private static final Set<String> REQUIRED_ENV_KEYS =
      Set.of("HUIFU_MERCHANT_ID", "HUIFU_RSA_PRIVATE_KEY", "HUIFU_WEBHOOK_ENDPOINT_KEY");

  private HuifuProviderDescriptor() {}

  public static HuifuProviderDescriptor create() {
    return new HuifuProviderDescriptor();
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String displayName() {
    return DISPLAY_NAME;
  }

  @Override
  public Set<String> requiredEnvKeys() {
    return REQUIRED_ENV_KEYS;
  }
}
