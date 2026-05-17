package com.imgltd.mmpay.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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

  @Test
  void returnsDuplicateForReplayedEventIdWithoutSendingAgain() {
    AtomicInteger deliveries = new AtomicInteger();
    WebhookDispatcher dispatcher =
        new WebhookDispatcher(
            WebhookSigner.withSecret("replace-with-test-secret"),
            new InMemoryWebhookIdempotencyStore(),
            message -> {
              deliveries.incrementAndGet();
              return WebhookTargetResponse.accepted();
            });
    Map<String, Object> payload =
        Map.of(
            "eventId", "evt_duplicate",
            "paymentIntentId", "pi_001",
            "status", "paid",
            "amountMinor", 1999,
            "occurredAt", Instant.EPOCH.toString());

    WebhookDeliveryResult first = dispatcher.dispatch(payload);
    WebhookDeliveryResult second = dispatcher.dispatch(payload);

    assertEquals(WebhookDeliveryStatus.SENT, first.status());
    assertEquals(WebhookDeliveryStatus.DUPLICATE, second.status());
    assertEquals(1, deliveries.get());
  }
}
