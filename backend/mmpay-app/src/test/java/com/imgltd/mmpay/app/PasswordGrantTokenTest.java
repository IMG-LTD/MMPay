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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import(TestAuditChainConfiguration.class)
class PasswordGrantTokenTest {
  private static final String CLIENT_ID = "mmpay-admin-console";
  private static final String CLIENT_SECRET = "replace-with-console-secret";
  private static final String PASSWORD = "Correct-Horse-1";
  private final ObjectMapper objectMapper = new ObjectMapper();
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void seedPasswordGrantClientAndAdmin() {
    jdbcTemplate.update(
        "INSERT INTO oauth2_registered_client (id, client_id, client_secret, client_name, "
            + "client_authentication_methods, authorization_grant_types, scopes, client_settings, token_settings) "
            + "VALUES (?, ?, ?, ?, 'client_secret_basic', 'password,refresh_token', 'role:admin', '{}', '{}')",
        UUID.randomUUID().toString(),
        CLIENT_ID,
        fingerprint(CLIENT_SECRET),
        CLIENT_ID);
    var userId = UUID.randomUUID().toString();
    jdbcTemplate.update(
        "INSERT INTO sys_user (id, username, kind, password_hash, role) VALUES (?, 'rootadmin', 'user', ?, 'admin')",
        userId,
        new BCryptPasswordEncoder().encode(PASSWORD));
    jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role) VALUES (?, 'admin')", userId);
  }

  @Test
  void passwordGrantIssuesOpaqueTokensAndRefreshTokenRotatesOnUse() throws Exception {
    var token = issuePasswordToken();
    var accessToken = token.get("access_token").asText();
    var refreshToken = token.get("refresh_token").asText();

    mockMvc.perform(get("/api/admin/audit").header("Authorization", "Bearer " + accessToken)).andExpect(status().isOk());

    var refreshed =
        mockMvc
            .perform(
                post("/oauth2/token")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "refresh_token")
                    .param("refresh_token", refreshToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token_type", is("Bearer")))
            .andExpect(jsonPath("$.refresh_token", not(blankOrNullString())))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var nextRefreshToken = objectMapper.readTree(refreshed).get("refresh_token").asText();
    assertThat(nextRefreshToken).isNotEqualTo(refreshToken);

    mockMvc
        .perform(
            post("/oauth2/token")
                .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken))
        .andExpect(status().isUnauthorized());
  }

  private com.fasterxml.jackson.databind.JsonNode issuePasswordToken() throws Exception {
    var response =
        mockMvc
            .perform(
                post("/oauth2/token")
                    .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .param("grant_type", "password")
                    .param("username", "rootadmin")
                    .param("password", PASSWORD))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token_type", is("Bearer")))
            .andExpect(jsonPath("$.access_token", not(blankOrNullString())))
            .andExpect(jsonPath("$.refresh_token", not(blankOrNullString())))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response);
  }

  private static String fingerprint(String value) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("fingerprint_failed", exception);
    }
  }
}
