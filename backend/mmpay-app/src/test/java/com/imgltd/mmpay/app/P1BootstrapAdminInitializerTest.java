package com.imgltd.mmpay.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import java.util.List;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@Import({TestAuditChainConfiguration.class, P1BootstrapAdminInitializerTest.FixedBootstrapAdminConfiguration.class})
class P1BootstrapAdminInitializerTest {
  private static final String HASH = "$2a$10$" + "a".repeat(53);

  @Autowired private List<ApplicationRunner> runners;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void envBootstrapCreatesAdminOnlyWhenMissing() throws Exception {
    assertEquals(1, adminRows());
    assertEquals(HASH, jdbcTemplate.queryForObject("SELECT password_hash FROM sys_user WHERE username = 'rootadmin'", String.class));

    for (ApplicationRunner runner : runners) {
      runner.run(new DefaultApplicationArguments());
    }

    assertEquals(1, adminRows());
  }

  private Integer adminRows() {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM sys_user WHERE username = 'rootadmin' AND role = 'admin' AND kind = 'user'",
        Integer.class);
  }

  @TestConfiguration
  static class FixedBootstrapAdminConfiguration {
    @Bean
    @Primary
    BootstrapAdminProperties fixedBootstrapAdminProperties() {
      return new BootstrapAdminProperties("rootadmin", HASH);
    }
  }
}
