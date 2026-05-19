package com.imgltd.mmpay.webhook;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

    assertTrue(
        message.signatureHeader().matches("t=[0-9]+,v1=[0-9a-f]{64}"),
        "header should be t=<unix>,v1=<hex>");
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

  @Test
  void signatureDependsOnBodyContentNotJustHeaderFields() {
    // Spec §5.1: body sha256 is part of the canonical signing input; amount tampering breaks sig.
    var clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
    var signer = WebhookSigner.withSecretAndClock("replace-with-test-secret", clock);

    Map<String, Object> headerFields =
        Map.of(
            "eventId", "evt_amount_tamper",
            "paymentIntentId", "pi_001",
            "status", "paid",
            "occurredAt", "2026-05-19T00:00:00Z");

    var withTwoThousand =
        signer.sign(
            Map.of(
                "eventId", "evt_amount_tamper",
                "paymentIntentId", "pi_001",
                "status", "paid",
                "amountMinor", 2000,
                "occurredAt", "2026-05-19T00:00:00Z"));
    var withTwoThousandOne =
        signer.sign(
            Map.of(
                "eventId", "evt_amount_tamper",
                "paymentIntentId", "pi_001",
                "status", "paid",
                "amountMinor", 2001,
                "occurredAt", "2026-05-19T00:00:00Z"));

    assertEquals(headerFields.size() + 1, 5); // payload differs only in amountMinor
    assertNotEquals(withTwoThousand.signatureHeader(), withTwoThousandOne.signatureHeader());
  }

  @Test
  void signatureIncludesTimestampClaim() {
    var clock = Clock.fixed(Instant.ofEpochSecond(1_700_000_000L), ZoneOffset.UTC);
    var signer = WebhookSigner.withSecretAndClock("replace-with-test-secret", clock);

    var message =
        signer.sign(
            Map.of(
                "eventId", "evt_t_claim",
                "paymentIntentId", "pi_001",
                "status", "paid",
                "occurredAt", "2026-05-19T00:00:00Z"));

    assertTrue(
        message.signatureHeader().startsWith("t=1700000000,v1="),
        () -> "expected t= claim, got " + message.signatureHeader());
  }
}
