package com.imgltd.mmpay.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import com.imgltd.mmpay.setup.SetupTokenService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import({TestAuditChainConfiguration.class, P1SetupRateLimitTest.FixedSetupTokenConfiguration.class})
class P1SetupRateLimitTest {
  private static final String CLIENT_IP = "203.0.113.8";

  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void createIamTables() {
    jdbcTemplate.execute("DROP TABLE IF EXISTS sys_user_role");
    jdbcTemplate.execute("DROP TABLE IF EXISTS sys_user");
    jdbcTemplate.execute(
        "CREATE TABLE sys_user (id VARCHAR(64) PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE, "
            + "kind VARCHAR(16) NOT NULL, password_hash VARCHAR(128), role VARCHAR(32) NOT NULL, "
            + "secret_fingerprint VARCHAR(64))");
    jdbcTemplate.execute(
        "CREATE TABLE sys_user_role (user_id VARCHAR(64) NOT NULL, role VARCHAR(32) NOT NULL, "
            + "PRIMARY KEY (user_id, role))");
  }

  @Test
  void sixthSetupPostFromSameIpIsRateLimited() throws Exception {
    for (int attempt = 0; attempt < 5; attempt++) {
      mockMvc.perform(post("/setup").with(request -> remoteAddress(request, CLIENT_IP))).andExpect(status().isUnauthorized());
    }

    mockMvc.perform(post("/setup").with(request -> remoteAddress(request, CLIENT_IP))).andExpect(status().isTooManyRequests());
  }

  private static MockHttpServletRequest remoteAddress(MockHttpServletRequest request, String remoteAddress) {
    request.setRemoteAddr(remoteAddress);
    return request;
  }

  @TestConfiguration
  static class FixedSetupTokenConfiguration {
    @Bean
    @Primary
    SetupTokenService fixedSetupTokenService() {
      return SetupTokenService.create(false, BootstrapAdminProperties.empty(), this::tokenBytes);
    }

    private byte[] tokenBytes() {
      return "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    }
  }
}
