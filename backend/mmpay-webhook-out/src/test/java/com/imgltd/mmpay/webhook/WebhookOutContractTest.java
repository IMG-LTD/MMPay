package com.imgltd.mmpay.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WebhookOutContractTest {
  @Test
  void signsPaymentFactPayloadWithMmmailHeaderFormat() {
    WebhookSigner signer = WebhookSigner.withSecret("replace-with-test-secret");
    WebhookMessage message =
        signer.sign(
            Map.of(
                "eventId", "evt_001",
                "paymentIntentId", "pi_001",
                "status", "paid",
                "amountMinor", 1999,
                "occurredAt", Instant.EPOCH.toString()));

    assertTrue(message.signatureHeader().matches("v1=[0-9a-f]{64}"));
    assertTrue(message.body().contains("\"eventId\":\"evt_001\""));
  }

  @Test
  void rejectsLicenseClaimFieldsInPayload() {
    WebhookSigner signer = WebhookSigner.withSecret("replace-with-test-secret");

    for (String field : LicenseClaimFieldNames.prohibitedFields()) {
      IllegalArgumentException error =
          assertThrows(
              IllegalArgumentException.class,
              () -> signer.sign(Map.of("eventId", "evt_claim", field, "blocked")));
      assertEquals("Webhook payload must not contain license claim field: " + field, error.getMessage());
    }
  }
}
