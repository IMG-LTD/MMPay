package com.imgltd.mmpay.huifu;

import java.util.Map;

public record HuifuSandboxCredentials(String merchantId, String apiKey, String webhookSecret) {
  private static final String MERCHANT_ID_ENV = "HUIFU_MERCHANT_ID";
  private static final String API_KEY_ENV = "HUIFU_API_KEY";
  private static final String WEBHOOK_SECRET_ENV = "HUIFU_WEBHOOK_SECRET";

  public HuifuSandboxCredentials {
    requireText(merchantId, MERCHANT_ID_ENV);
    requireText(apiKey, API_KEY_ENV);
    requireText(webhookSecret, WEBHOOK_SECRET_ENV);
  }

  public static HuifuSandboxCredentials from(Map<String, String> env) {
    return new HuifuSandboxCredentials(
        envValue(env, MERCHANT_ID_ENV), envValue(env, API_KEY_ENV), envValue(env, WEBHOOK_SECRET_ENV));
  }

  public HuifuCredentialHandles toCredentialHandles() {
    return new HuifuCredentialHandles(merchantId, apiKey, webhookSecret);
  }

  private static String envValue(Map<String, String> env, String name) {
    if (env == null || !env.containsKey(name)) {
      throw new IllegalStateException("Missing required Huifu sandbox environment variable: " + name);
    }
    return requireText(env.get(name), name);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(fieldName + " must not be blank");
    }
    return value;
  }
}
