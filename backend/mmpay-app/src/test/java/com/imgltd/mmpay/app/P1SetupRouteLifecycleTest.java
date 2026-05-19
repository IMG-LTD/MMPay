package com.imgltd.mmpay.app;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@AutoConfigureMockMvc
@Import({
  TestAuditChainConfiguration.class,
  TestSetupBootstrapLockConfiguration.class,
  P1SetupRouteLifecycleTest.FixedSetupTokenConfiguration.class
})
class P1SetupRouteLifecycleTest {
  private static final String TOKEN = "3031323334353637383961626364656630313233343536373839616263646566";

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
  void setupCreatesTheOnlyBootstrapAdminAndThenDisappears() throws Exception {
    mockMvc
        .perform(get("/setup"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("MMPay setup")));

    mockMvc.perform(post("/setup").param("username", "admin")).andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            post("/setup")
                .header("X-Setup-Token", TOKEN)
                .param("username", "admin")
                .param("password", "MMPayAdmin1!")
                .param("passwordConfirm", "MMPayAdmin1!")
                .param("initialLocale", "zh-CN"))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", "/login"));

    Integer adminRows =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE username = 'admin' AND role = 'admin' AND kind = 'user'",
            Integer.class);
    String passwordHash =
        jdbcTemplate.queryForObject("SELECT password_hash FROM sys_user WHERE username = 'admin'", String.class);
    assertEquals(1, adminRows);
    assertTrue(passwordHash.startsWith("$2"));

    mockMvc.perform(get("/setup")).andExpect(status().isNotFound());
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
