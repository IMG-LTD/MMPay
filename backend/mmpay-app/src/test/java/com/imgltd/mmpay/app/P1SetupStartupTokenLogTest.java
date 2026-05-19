package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import com.imgltd.mmpay.setup.SetupTokenService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@ExtendWith(OutputCaptureExtension.class)
@Import({TestAuditChainConfiguration.class, P1SetupStartupTokenLogTest.FixedSetupTokenConfiguration.class})
class P1SetupStartupTokenLogTest {
  private static final String TOKEN = "3031323334353637383961626364656630313233343536373839616263646566";

  @Test
  void firstBootPrintsSetupTokenWhenNoAdminExists(CapturedOutput output) {
    assertThat(output.getOut()).contains("mmpay-setup-token: " + TOKEN);
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
