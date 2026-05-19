package com.imgltd.mmpay.app;

import com.imgltd.mmpay.audit.AuditChain;
import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.audit.AuditHasher;
import com.imgltd.mmpay.audit.InMemoryAuditEventStore;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
class TestAuditChainConfiguration {
  private static final byte[] TEST_KEY = "replace-with-test-audit-key-material".getBytes(StandardCharsets.UTF_8);

  @Bean
  AuditHasher testAuditHasher() {
    return new AuditHasher(TEST_KEY);
  }

  @Bean
  AuditChain testAuditChain(AuditHasher auditHasher) {
    return new AuditChain(auditHasher, java.time.Clock.systemUTC());
  }

  @Bean
  AuditEventStore testAuditEventStore(AuditChain auditChain) {
    return new InMemoryAuditEventStore(auditChain);
  }
}
