package com.imgltd.mmpay.huifu;

import java.util.Map;

public record HuifuSandboxCredentials(
    String sysId,
    String productId,
    String rsaPublicKey,
    String rsaPrivateKey,
    String skillSource,
    String merchantId,
    String notifyUrl,
    String webhookEndpointKey,
    String sdkRoot) {
  private static final String SYS_ID_ENV = "HUIFU_SYS_ID";
  private static final String PRODUCT_ID_ENV = "HUIFU_PRODUCT_ID";
  private static final String RSA_PUBLIC_KEY_ENV = "HUIFU_RSA_PUBLIC_KEY";
  private static final String RSA_PRIVATE_KEY_ENV = "HUIFU_RSA_PRIVATE_KEY";
  private static final String SKILL_SOURCE_ENV = "HUIFU_SKILL_SOURCE";
  private static final String MERCHANT_ID_ENV = "HUIFU_MERCHANT_ID";
  private static final String NOTIFY_URL_ENV = "HUIFU_NOTIFY_URL";
  private static final String WEBHOOK_ENDPOINT_KEY_ENV = "HUIFU_WEBHOOK_ENDPOINT_KEY";
  private static final String SDK_ROOT_ENV = "HUIFU_SDK_ROOT";

  public HuifuSandboxCredentials {
    requireText(sysId, SYS_ID_ENV);
    requireText(productId, PRODUCT_ID_ENV);
    requireText(rsaPublicKey, RSA_PUBLIC_KEY_ENV);
    requireText(rsaPrivateKey, RSA_PRIVATE_KEY_ENV);
    requireText(skillSource, SKILL_SOURCE_ENV);
    requireText(merchantId, MERCHANT_ID_ENV);
    requireText(notifyUrl, NOTIFY_URL_ENV);
    webhookEndpointKey = webhookEndpointKey == null ? "" : webhookEndpointKey;
    sdkRoot = sdkRoot == null ? "" : sdkRoot;
  }

  public static HuifuSandboxCredentials from(Map<String, String> env) {
    return new HuifuSandboxCredentials(
        envValue(env, SYS_ID_ENV),
        envValue(env, PRODUCT_ID_ENV),
        envValue(env, RSA_PUBLIC_KEY_ENV),
        envValue(env, RSA_PRIVATE_KEY_ENV),
        envValue(env, SKILL_SOURCE_ENV),
        envValue(env, MERCHANT_ID_ENV),
        envValue(env, NOTIFY_URL_ENV),
        optionalEnvValue(env, WEBHOOK_ENDPOINT_KEY_ENV),
        optionalEnvValue(env, SDK_ROOT_ENV));
  }

  public HuifuCredentialHandles toCredentialHandles() {
    return new HuifuCredentialHandles(
        "env://" + MERCHANT_ID_ENV, "env://" + RSA_PRIVATE_KEY_ENV, "env://" + WEBHOOK_ENDPOINT_KEY_ENV);
  }

  private static String envValue(Map<String, String> env, String name) {
    if (env == null || !env.containsKey(name)) {
      throw new IllegalStateException("Missing required Huifu sandbox environment variable: " + name);
    }
    return requireText(env.get(name), name);
  }

  private static String optionalEnvValue(Map<String, String> env, String name) {
    if (env == null) {
      return "";
    }
    return env.getOrDefault(name, "");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(fieldName + " must not be blank");
    }
    return value;
  }
}
