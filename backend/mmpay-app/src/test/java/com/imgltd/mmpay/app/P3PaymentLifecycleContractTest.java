package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql,classpath:/test-p3-payment-schema.sql",
      "mmpay.provider-live-calls=false",
      "mmpay.provider-callback-secret=callback-secret"
    })
@AutoConfigureMockMvc
@Import({TestAuditChainConfiguration.class, P3PaymentLifecycleContractTest.TestResolverConfig.class})
class P3PaymentLifecycleContractTest {
  private static final Instant NOW = Instant.parse("2026-05-19T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AuditEventStore auditEvents;

  @Test
  @WithMockUser(username = "ops", roles = "OPS")
  void providerDisabledCreateFailsClosedAfterUseTimeCredentialCheck() throws Exception {
    seedMerchantAndChannel("m_live_off", "ch_live_off", "env://HUIFU_CHANNEL_KEY", "b6b290a5");

    mockMvc
        .perform(
            post("/api/admin/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .content(createIntentJson("ch_live_off", 1999)))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:provider-live-disabled")));

    assertAudit("payment_intent.create", "payment_intent", "rejected");
  }

  @Test
  @WithMockUser(username = "ops", roles = "OPS")
  void staleChannelFingerprintBlocksCredentialUseBeforeProviderCall() throws Exception {
    seedMerchantAndChannel("m_stale_use", "ch_stale_use", "env://HUIFU_CHANNEL_KEY", "00000000");

    mockMvc
        .perform(
            post("/api/admin/payment-intents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .content(createIntentJson("ch_stale_use", 1999)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:fingerprint-rebind-required")));

    assertAudit("credential_ref.mismatch_detected", "channel", "rejected");
  }

  @Test
  @WithMockUser(username = "ops", roles = "OPS")
  void readsCancelsAndRejectsCallbacksAfterOperatorCancel() throws Exception {
    seedMerchantAndChannel("m_cancel", "ch_cancel", "env://HUIFU_CHANNEL_KEY", "b6b290a5");
    seedPaymentIntent("pi_cancel", "m_cancel", "ch_cancel", "pending", 1999);

    mockMvc.perform(get("/api/admin/payment-intents")).andExpect(jsonPath("$.items[*].id", hasItem("pi_cancel")));
    mockMvc
        .perform(post("/api/admin/payment-intents/pi_cancel/cancel"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("cancelled")));

    String callbackBody = callbackJson("evt_cancel", "pi_cancel", "succeeded", 1999);
    mockMvc
        .perform(
            post("/webhook-in/huifu")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-MMPay-Provider-Signature", sign(callbackBody))
                .content(callbackBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:state-transition-illegal")));

    assertAudit("payment_intent.state_inconsistent", "payment_intent", "rejected");
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void refundUpperBoundAndReconciliationAckAreExplicit() throws Exception {
    seedMerchantAndChannel("m_refund", "ch_refund", "env://HUIFU_CHANNEL_KEY", "b6b290a5");
    seedPaymentIntent("pi_refund", "m_refund", "ch_refund", "succeeded", 1000);
    seedRefund("rf_existing", "pi_refund", "m_refund", 700, "pending");
    seedReconciliationRun("ch_refund");

    mockMvc
        .perform(post("/api/admin/refunds").contentType(MediaType.APPLICATION_JSON).content(refundJson("pi_refund", 400)))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:refund-exceeds-intent")));

    mockMvc.perform(get("/api/admin/reconciliation/runs")).andExpect(jsonPath("$.items[*].channel_id", hasItem("ch_refund")));
    mockMvc
        .perform(post("/api/admin/reconciliation/runs/1/ack"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ack_status", is("acked")));
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void webhookOutRejectsSsrfTargetsAndBulkRedispatchOverflow() throws Exception {
    seedDeliveryLog("wh_good", "pi_delivery", "evt-delivery");

    mockMvc
        .perform(get("/api/admin/webhook-out/delivery-logs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].integration_id", hasItem("wh_good")));

    mockMvc
        .perform(post("/api/admin/webhook-out/delivery-logs/1/redispatch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attempt", is(1)))
        .andExpect(jsonPath("$.event_id", is("evt-delivery")));

    mockMvc
        .perform(
            post("/api/admin/webhook-out/integrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webhookIntegrationJson("wh_bad", "http://127.0.0.1/hook")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:target-url-rejected")));

    mockMvc
        .perform(
            post("/api/admin/webhook-out/bulk-redispatch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"integration_id\":\"wh_any\",\"event_count\":10001,\"rps\":10}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:bulk-redispatch-too-large")));
  }

  private void seedMerchantAndChannel(String merchantId, String channelId, String ref, String fingerprint) {
    jdbcTemplate.update(
        "INSERT INTO merchants VALUES (?, ?, ?, ?, ?, ?, 'active', 'default', ?, ?)",
        uuid(), merchantId, merchantId, "env://ACME_MERCHANT_KEY", "env://ACME_MERCHANT_KEY", "307c609f", ts(), ts());
    jdbcTemplate.update(
        "INSERT INTO channels VALUES (?, ?, ?, ?, 'huifu', ?, ?, ?, 'active', 'default', ?, ?)",
        uuid(), channelId, merchantId, channelId, ref, ref, fingerprint, ts(), ts());
  }

  private void seedPaymentIntent(String id, String merchantId, String channelId, String status, long amount) {
    jdbcTemplate.update(
        "INSERT INTO payment_intents VALUES (?, ?, ?, ?, ?, ?, 'CNY', ?, ?, ?, 'default', ?, ?, 0)",
        id, merchantId, channelId, null, null, amount, "order-" + id, "idem-" + id, status, ts(), ts());
  }

  private void seedRefund(String id, String intentId, String merchantId, long amount, String status) {
    jdbcTemplate.update(
        "INSERT INTO refunds VALUES (?, NULL, ?, ?, ?, 'CNY', ?, 'default', ?, 0)",
        id, intentId, merchantId, amount, status, ts());
  }

  private void seedReconciliationRun(String channelId) {
    jdbcTemplate.update(
        "INSERT INTO reconciliation_runs VALUES (1, ?, 'huifu', ?, 0, 0, 0, 'silent', 'pending', NULL, NULL, 'default')",
        LocalDate.parse("2026-05-18"), channelId);
  }

  private void seedDeliveryLog(String integrationId, String intentId, String eventId) {
    jdbcTemplate.update(
        "INSERT INTO delivery_logs (integration_id, payment_intent_id, event_id, attempt, scheduled_at, response_status, "
            + "next_retry_at, dead_letter, tenant_id) VALUES (?, ?, ?, 3, ?, 500, ?, true, 'default')",
        integrationId,
        intentId,
        eventId,
        ts(),
        Timestamp.from(NOW.plusSeconds(60)));
  }

  private void assertAudit(String action, String targetKind, String result) {
    assertThat(auditEvents.events())
        .anySatisfy(
            event -> {
              assertThat(event.action()).isEqualTo(action);
              assertThat(event.targetKind()).isEqualTo(targetKind);
              assertThat(event.details().get("result")).isEqualTo(result);
            });
  }

  private static String callbackJson(String eventId, String intentId, String status, long amount) {
    return """
        {"provider_event_id":"%s","payment_intent_id":"%s","status":"%s","amount_minor":%d,"occurred_at":"2026-05-19T00:01:00Z"}
        """
        .formatted(eventId, intentId, status, amount);
  }

  private static String createIntentJson(String channelId, long amount) {
    return """
        {"channel_id":"%s","amount_minor":%d,"currency":"CNY","order_ref":"order-p3"}
        """
        .formatted(channelId, amount);
  }

  private static String refundJson(String intentId, long amount) {
    return "{\"payment_intent_id\":\"%s\",\"amount_minor\":%d}".formatted(intentId, amount);
  }

  private static String webhookIntegrationJson(String id, String targetUrl) {
    return "{\"id\":\"%s\",\"display_name\":\"Bad\",\"target_url\":\"%s\",\"secret_ref\":\"env://WEBHOOK_SECRET\"}"
        .formatted(id, targetUrl);
  }

  private static String sign(String body) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec("callback-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }

  private static String uuid() {
    return UUID.randomUUID().toString();
  }

  private static Timestamp ts() {
    return Timestamp.from(NOW);
  }

  @TestConfiguration
  static class TestResolverConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC);
    }

    @Bean
    @Primary
    EnvironmentReferenceResolver testEnvironmentReferenceResolver() {
      return new EnvironmentReferenceResolver(
          Map.of("ACME_MERCHANT_KEY", "acme-secret", "HUIFU_CHANNEL_KEY", "huifu-secret", "WEBHOOK_SECRET", "hook-secret"));
    }
  }
}
