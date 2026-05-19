package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import(TestAuditChainConfiguration.class)
class ServicePrincipalGrantTest {
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;

  @Test
  @WithMockUser(roles = "ADMIN")
  void createsServicePrincipalAndIssuesBoundClientCredentialsToken() throws Exception {
    var principal =
        mockMvc
            .perform(
                post("/api/admin/service-principals")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"ops-sync\",\"role\":\"ops\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.client_id", not(blankOrNullString())))
            .andExpect(jsonPath("$.client_secret", not(blankOrNullString())))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var created = objectMapper.readTree(principal);
    var clientId = created.get("client_id").asText();
    var clientSecret = created.get("client_secret").asText();

    assertThat(storedSecretFingerprint(clientId)).isNotEqualTo(clientSecret);

    var token =
        mockMvc
            .perform(
                post("/oauth2/token")
                    .with(httpBasic(clientId, clientSecret))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "client_credentials"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token_type", is("Bearer")))
            .andExpect(jsonPath("$.access_token", not(blankOrNullString())))
            .andReturn()
            .getResponse()
            .getContentAsString();
    var accessToken = objectMapper.readTree(token).get("access_token").asText();

    mockMvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + accessToken)).andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + accessToken)).andExpect(status().isForbidden());
  }

  private String storedSecretFingerprint(String clientId) {
    return jdbcTemplate.queryForObject(
        "SELECT secret_fingerprint FROM sys_user WHERE username = ?", String.class, clientId);
  }
}
