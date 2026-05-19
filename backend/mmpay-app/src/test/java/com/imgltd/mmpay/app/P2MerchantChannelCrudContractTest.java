package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql,classpath:/test-p2-gateway-schema.sql"
    })
@AutoConfigureMockMvc
@Import({TestAuditChainConfiguration.class, P2MerchantChannelCrudContractTest.TestResolverConfig.class})
class P2MerchantChannelCrudContractTest {
  private static final String IDEMPOTENCY_KEY = "11111111-1111-4111-8111-111111111111";

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditEventStore auditEvents;

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void adminCreatesMerchantAndHuifuChannelWithFingerprint() throws Exception {
    createMerchant("m_acme", "Acme", "env://ACME_MERCHANT_KEY")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is("m_acme")))
        .andExpect(jsonPath("$.credential_ref", is("env://ACME_MERCHANT_KEY")))
        .andExpect(jsonPath("$.credential_fingerprint", is("307c609f")));

    mockMvc
        .perform(get("/api/admin/merchants"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[*].id", hasItem("m_acme")));

    mockMvc
        .perform(
            post("/api/admin/merchants/m_acme/channels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"id":"ch_huifu","display_name":"Huifu default","provider_code":"huifu","credential_ref":"env://HUIFU_CHANNEL_KEY"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.provider_code", is("huifu")))
        .andExpect(jsonPath("$.credential_fingerprint", is("b6b290a5")));
  }

  @Test
  @WithMockUser(username = "ops", roles = "OPS")
  void opsCanReadButCannotWrite() throws Exception {
    mockMvc.perform(get("/api/admin/merchants")).andExpect(status().isOk());
    createMerchant("m_ops", "Ops", "env://ACME_MERCHANT_KEY").andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void rejectedCredentialBindReturnsProblemAndAuditRow() throws Exception {
    createMerchant("m_missing", "Missing", "env://MISSING_KEY")
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:credential-ref-invalid")));

    assertThat(auditEvents.events())
        .anySatisfy(
            event -> {
              assertThat(event.action()).isEqualTo("credential_ref.bind");
              assertThat(event.details().get("result")).isEqualTo("rejected");
              assertThat(event.details().get("reason")).isEqualTo("env_unset");
            });
  }

  @Test
  @WithMockUser(username = "admin", roles = "ADMIN")
  void idempotencyKeyReplaysOriginalPostResponseAndRejectsMismatch() throws Exception {
    createMerchantWithKey("m_idem", "Idem", "env://ACME_MERCHANT_KEY", IDEMPOTENCY_KEY)
        .andExpect(status().isCreated());
    createMerchantWithKey("m_idem", "Idem", "env://ACME_MERCHANT_KEY", IDEMPOTENCY_KEY)
        .andExpect(status().isCreated());
    createMerchantWithKey("m_other", "Other", "env://ACME_MERCHANT_KEY", IDEMPOTENCY_KEY)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type", is("urn:mmpay:problem:idempotency-key-replay-mismatch")));
  }

  private org.springframework.test.web.servlet.ResultActions createMerchant(String id, String name, String ref)
      throws Exception {
    return createMerchantWithKey(id, name, ref, null);
  }

  private org.springframework.test.web.servlet.ResultActions createMerchantWithKey(
      String id, String name, String ref, String key) throws Exception {
    var request =
        post("/api/admin/merchants")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"id":"%s","display_name":"%s","credential_ref":"%s"}
                """
                    .formatted(id, name, ref));
    return mockMvc.perform(key == null ? request : request.header("Idempotency-Key", key));
  }

  @TestConfiguration
  static class TestResolverConfig {
    @Bean
    @Primary
    EnvironmentReferenceResolver testEnvironmentReferenceResolver() {
      return new EnvironmentReferenceResolver(
          Map.of("ACME_MERCHANT_KEY", "acme-secret", "HUIFU_CHANNEL_KEY", "huifu-secret"));
    }
  }
}
