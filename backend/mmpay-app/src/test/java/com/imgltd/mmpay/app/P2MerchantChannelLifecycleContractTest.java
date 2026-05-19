package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
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
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql,classpath:/test-p2-gateway-schema.sql"
    })
@AutoConfigureMockMvc
@Import({TestAuditChainConfiguration.class, P2MerchantChannelLifecycleContractTest.TestResolverConfig.class})
class P2MerchantChannelLifecycleContractTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AuditEventStore auditEvents;

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void adminPatchesVerifiesAndArchivesMerchant() throws Exception {
    createMerchant("m_patch", "Patch Me", "env://ACME_MERCHANT_KEY");

    mockMvc
        .perform(
            patch("/api/admin/merchants/m_patch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"display_name":"Patch Done","credentialRef":{"type":"Set","value":"env://ROTATED_MERCHANT_KEY"}}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.display_name", is("Patch Done")))
        .andExpect(jsonPath("$.credential_ref", is("env://ROTATED_MERCHANT_KEY")))
        .andExpect(jsonPath("$.credential_fingerprint", is("a26fa50c")));

    mockMvc
        .perform(post("/api/admin/merchants/m_patch/verify-binding"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("current")));

    mockMvc.perform(delete("/api/admin/merchants/m_patch")).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/admin/merchants/m_patch")).andExpect(status().isConflict());
    assertAudit("merchant.update", "merchant", "m_patch", "accepted");
    assertAudit("merchant.delete", "merchant", "m_patch", "accepted");
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void adminUnbindsAndArchivesChannel() throws Exception {
    createMerchant("m_channel_patch", "Channel Patch", "env://ACME_MERCHANT_KEY");
    createChannel("m_channel_patch", "ch_patch", "env://HUIFU_CHANNEL_KEY");

    mockMvc
        .perform(
            patch("/api/admin/channels/ch_patch")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credentialRef\":{\"type\":\"Unbind\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.credential_ref", nullValue()))
        .andExpect(jsonPath("$.credential_fingerprint", nullValue()));

    mockMvc.perform(delete("/api/admin/channels/ch_patch")).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/admin/channels/ch_patch")).andExpect(status().isConflict());
    assertAudit("credential_ref.unbind", "channel", "ch_patch", "accepted");
    assertAudit("channel.delete", "channel", "ch_patch", "accepted");
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void verifyBindingMismatchReturnsProblemAndAuditRow() throws Exception {
    insertMerchantWithFingerprint("m_stale", "env://ACME_MERCHANT_KEY", "00000000");

    mockMvc
        .perform(post("/api/admin/merchants/m_stale/verify-binding"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:fingerprint-rebind-required")));

    assertThat(auditEvents.events())
        .anySatisfy(
            event -> {
              assertThat(event.action()).isEqualTo("credential_ref.bind");
              assertThat(event.targetKind()).isEqualTo("merchant");
              assertThat(event.targetId()).isEqualTo("m_stale");
              assertThat(event.details().get("verify_result")).isEqualTo("stale");
            });
  }

  private void createMerchant(String id, String name, String ref) throws Exception {
    mockMvc
        .perform(
            post("/api/admin/merchants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"%s","display_name":"%s","credential_ref":"%s"}
                    """
                        .formatted(id, name, ref)))
        .andExpect(status().isCreated());
  }

  private void createChannel(String merchantId, String id, String ref) throws Exception {
    mockMvc
        .perform(
            post("/api/admin/merchants/%s/channels".formatted(merchantId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"%s","display_name":"Channel","provider_code":"huifu","credential_ref":"%s"}
                    """
                        .formatted(id, ref)))
        .andExpect(status().isCreated());
  }

  private void insertMerchantWithFingerprint(String id, String ref, String fingerprint) {
    jdbcTemplate.update(
        "INSERT INTO merchants (row_uid, id, display_name, credential_handle, credential_ref, credential_fingerprint, "
            + "status, tenant_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'active', 'default', ?, ?)",
        UUID.randomUUID().toString(),
        id,
        "Stale",
        ref,
        ref,
        fingerprint,
        Timestamp.from(Instant.parse("2026-05-19T00:00:00Z")),
        Timestamp.from(Instant.parse("2026-05-19T00:00:00Z")));
  }

  private void assertAudit(String action, String kind, String id, String result) {
    assertThat(auditEvents.events())
        .anySatisfy(
            event -> {
              assertThat(event.action()).isEqualTo(action);
              assertThat(event.targetKind()).isEqualTo(kind);
              assertThat(event.targetId()).isEqualTo(id);
              assertThat(event.details().get("result")).isEqualTo(result);
            });
  }

  @TestConfiguration
  static class TestResolverConfig {
    @Bean
    @Primary
    EnvironmentReferenceResolver testEnvironmentReferenceResolver() {
      return new EnvironmentReferenceResolver(
          Map.of(
              "ACME_MERCHANT_KEY",
              "acme-secret",
              "HUIFU_CHANNEL_KEY",
              "huifu-secret",
              "ROTATED_MERCHANT_KEY",
              "rotated-secret"));
    }
  }
}
