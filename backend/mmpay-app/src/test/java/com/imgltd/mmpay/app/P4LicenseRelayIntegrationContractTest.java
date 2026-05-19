package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.audit.AuditEventStore;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql,classpath:/test-p4-relay-schema.sql",
      "mmpay.relay.trust-header=X-Client-Cert",
      "mmpay.relay.allow-local-targets=true",
      "mmpay.relay.port=9443",
      "mmpay.relay.rate-limit-per-cert=2",
      "mmpay.provider-live-calls=true",
      "mmpay.provider-callback-secret=callback-secret"
    })
@AutoConfigureMockMvc
@Import({TestAuditChainConfiguration.class, P4LicenseRelayIntegrationContractTest.ClockConfig.class})
class P4LicenseRelayIntegrationContractTest {
  private static final Instant NOW = Instant.parse("2026-05-19T00:00:00Z");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AuditEventStore auditEvents;

  private HttpServer upstream;
  private byte[] upstreamPayload = new byte[0];

  @BeforeEach
  void startUpstream() throws Exception {
    resetRelayTables();
    upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    upstream.createContext(
        "/license",
        exchange -> {
          upstreamPayload = exchange.getRequestBody().readAllBytes();
          byte[] response = "accepted-response-body".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
    upstream.start();
  }

  @AfterEach
  void stopUpstream() {
    upstream.stop(0);
  }

  @Test
  void adminPortDoesNotExposeRelayForwardRoute() throws Exception {
    mockMvc
        .perform(
            post("/api/license-relay/v1/forward")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-License-Relay-Target-Id", "relay-prod")
                .header("X-Request-Id", "req-admin-port")
                .content("opaque"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void createsRelayIntegrationMasksTargetAndForwardsOpaqueBytes() throws Exception {
    createRelayIntegration("relay-prod", "upstream-prod", upstreamUrl());

    mockMvc.perform(get("/api/admin/integrations")).andExpect(jsonPath("$.items[*].slug", hasItem("upstream-prod")));
    mockMvc
        .perform(get("/api/admin/integrations/relay-prod"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.target_url_masked", is(upstreamHost() + "/[REDACTED]")));

    mockMvc
        .perform(
            post("/api/license-relay/v1/forward")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Client-Cert", "CN=vendor-forward,O=IMG-LTD")
                .header("X-License-Relay-Target-Id", "relay-prod")
                .header("X-Request-Id", "req-forward")
                .content("opaque-vendor-signed-license"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.target_id", is("relay-prod")))
        .andExpect(jsonPath("$.byte_count", is(28)))
        .andExpect(jsonPath("$.synthetic", is(false)));

    assertThat(new String(upstreamPayload, StandardCharsets.UTF_8)).isEqualTo("opaque-vendor-signed-license");
    assertAudit("license_relay.forwarded", "license_relay", "accepted");
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void rejectsReservedSlugsOversizePayloadAndRateLimitExhaustion() throws Exception {
    createRelayIntegration("relay-rate", "upstream-prod", upstreamUrl());

    mockMvc
        .perform(
            post("/api/admin/integrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(relayIntegrationJson("relay-admin", "billing-admin", upstreamUrl())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:integration-slug-reserved")));

    mockMvc
        .perform(
            post("/api/license-relay/v1/forward")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Client-Cert", "CN=vendor-rate,O=IMG-LTD")
                .header("X-License-Relay-Target-Id", "relay-rate")
                .header("X-Request-Id", "req-large")
                .content(new byte[65537]))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:relay-payload-too-large")));

    forward("relay-rate", "req-1");
    forward("relay-rate", "req-2");
    mockMvc
        .perform(
            post("/api/license-relay/v1/forward")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Client-Cert", "CN=vendor-rate,O=IMG-LTD")
                .header("X-License-Relay-Target-Id", "relay-rate")
                .header("X-Request-Id", "req-3")
                .content("opaque"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:relay-rate-limited")));
  }

  @Test
  @WithMockUser(username = "ops", roles = "OPS")
  void syntheticTestAndPerRowRedispatchStaySeparateFromVendorForwardedAudit() throws Exception {
    createRelayIntegration("relay-test", "upstream-prod", upstreamUrl());

    mockMvc
        .perform(post("/api/admin/integrations/relay-test/test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.synthetic", is(true)));
    mockMvc
        .perform(post("/api/admin/license-relay/logs/1/redispatch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attempt", is(2)));

    assertAudit("integration.test_invoked", "integration", "accepted");
    assertAudit("license_relay.synthetic_forwarded", "license_relay", "accepted");
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void evidenceSnapshotRejectsMissingForwardedRelayFacts() throws Exception {
    mockMvc
        .perform(post("/api/admin/evidence/snapshot").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:evidence-precheck-failed")));
  }

  private void createRelayIntegration(String id, String slug, String targetUrl) throws Exception {
    mockMvc
        .perform(post("/api/admin/integrations").contentType(MediaType.APPLICATION_JSON).content(relayIntegrationJson(id, slug, targetUrl)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(id)));
  }

  private void forward(String targetId, String requestId) throws Exception {
    mockMvc
        .perform(
            post("/api/license-relay/v1/forward")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("X-Client-Cert", "CN=vendor-rate,O=IMG-LTD")
                .header("X-License-Relay-Target-Id", targetId)
                .header("X-Request-Id", requestId)
                .content("opaque"))
        .andExpect(status().isAccepted());
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

  private void resetRelayTables() {
    jdbcTemplate.update("DELETE FROM license_relay_logs");
    jdbcTemplate.update("DELETE FROM webhook_integrations");
    jdbcTemplate.update("DELETE FROM license_relay_targets");
    jdbcTemplate.execute("ALTER TABLE license_relay_logs ALTER COLUMN id RESTART WITH 1");
  }

  private String relayIntegrationJson(String id, String slug, String targetUrl) {
    return """
        {"id":"%s","kind":"relay","name":"Upstream","slug":"%s","target_url":"%s","secret_ref":"env://UPSTREAM_SECRET"}
        """
        .formatted(id, slug, targetUrl);
  }

  private String upstreamUrl() {
    return upstreamHost() + "/license?token=hidden";
  }

  private String upstreamHost() {
    return "http://127.0.0.1:" + upstream.getAddress().getPort();
  }

  @TestConfiguration
  static class ClockConfig {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW, ZoneOffset.UTC);
    }
  }
}
