package com.imgltd.mmpay.huifu;

public record HuifuCredentialHandles(String merchantIdHandle, String apiKeyHandle, String webhookSecretHandle) {
  public HuifuCredentialHandles {
    requireText(merchantIdHandle, "merchantIdHandle");
    requireText(apiKeyHandle, "apiKeyHandle");
    requireText(webhookSecretHandle, "webhookSecretHandle");
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
    return value;
  }
}
