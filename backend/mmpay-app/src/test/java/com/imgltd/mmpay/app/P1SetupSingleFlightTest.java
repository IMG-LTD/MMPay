package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.imgltd.mmpay.setup.SetupAlreadyCompletedException;
import com.imgltd.mmpay.setup.SetupBootstrapLock;
import com.imgltd.mmpay.setup.SetupBootstrapService;
import com.imgltd.mmpay.setup.SetupRequest;
import java.util.concurrent.atomic.AtomicInteger;
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
@Import({TestAuditChainConfiguration.class, P1SetupSingleFlightTest.LockConfiguration.class})
class P1SetupSingleFlightTest {
  @Autowired private CountingSetupBootstrapLock lock;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private SetupBootstrapService bootstrapService;

  @Test
  void secondBootstrapAttemptIsRejectedAfterSingleFlightLock() {
    bootstrapService.createAdmin(request("adminone"));

    assertThatThrownBy(() -> bootstrapService.createAdmin(request("admintwo")))
        .isInstanceOf(SetupAlreadyCompletedException.class)
        .hasMessage("setup_already_completed");

    assertThat(adminRows()).isEqualTo(1);
    assertThat(lock.acquisitions()).isEqualTo(2);
  }

  private Integer adminRows() {
    return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user WHERE role = 'admin' AND kind = 'user'", Integer.class);
  }

  private SetupRequest request(String username) {
    return new SetupRequest(username, "MMPayAdmin1!", "MMPayAdmin1!", "zh-CN");
  }

  @TestConfiguration
  static class LockConfiguration {
    @Bean
    @Primary
    CountingSetupBootstrapLock setupBootstrapLock() {
      return new CountingSetupBootstrapLock();
    }
  }

  static class CountingSetupBootstrapLock implements SetupBootstrapLock {
    private final AtomicInteger acquisitions = new AtomicInteger();

    @Override
    public void acquire() {
      acquisitions.incrementAndGet();
    }

    int acquisitions() {
      return acquisitions.get();
    }
  }
}
