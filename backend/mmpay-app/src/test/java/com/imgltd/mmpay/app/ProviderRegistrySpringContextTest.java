package com.imgltd.mmpay.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.imgltd.mmpay.adapter.ProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    properties = {
      "spring.datasource.hikari.connection-init-sql=",
      "spring.sql.init.mode=always",
      "spring.sql.init.schema-locations=classpath:/test-iam-schema.sql"
    })
@Import(TestAuditChainConfiguration.class)
class ProviderRegistrySpringContextTest {
  @Autowired private ProviderRegistry providerRegistry;

  @Test
  void registersHuifuDescriptorInApplicationContext() {
    var descriptor = providerRegistry.require("huifu");

    assertEquals("Huifu", descriptor.displayName());
  }
}
