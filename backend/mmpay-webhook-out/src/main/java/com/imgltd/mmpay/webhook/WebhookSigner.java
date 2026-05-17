package com.imgltd.mmpay.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.Map;

public final class WebhookSigner {
  private static final String HEADER_VERSION = "v1=";
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

  private final HmacSha256Signer hmacSigner;

  private WebhookSigner(String secret) {
    this.hmacSigner = new HmacSha256Signer(secret);
  }

  public static WebhookSigner withSecret(String secret) {
    return new WebhookSigner(secret);
  }

  public WebhookMessage sign(Map<String, ?> payload) {
    rejectLicenseClaimFields(payload);
    String body = toJson(payload);
    return new WebhookMessage(body, HEADER_VERSION + hmacSigner.signHex(body));
  }

  private static void rejectLicenseClaimFields(Map<String, ?> payload) {
    for (String field : LicenseClaimFieldNames.prohibitedFields()) {
      if (payload.containsKey(field)) {
        throw new IllegalArgumentException(
            "Webhook payload must not contain license claim field: " + field);
      }
    }
  }

  private static String toJson(Map<String, ?> payload) {
    try {
      return OBJECT_MAPPER.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      throw new IllegalArgumentException("webhook payload is not serializable", exception);
    }
  }
}
