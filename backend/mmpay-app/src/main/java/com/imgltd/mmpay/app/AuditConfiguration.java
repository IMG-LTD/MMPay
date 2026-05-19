package com.imgltd.mmpay.app;

import com.imgltd.mmpay.audit.AuditChain;
import com.imgltd.mmpay.audit.AuditEventStore;
import com.imgltd.mmpay.audit.AuditHasher;
import com.imgltd.mmpay.audit.AuditLock;
import com.imgltd.mmpay.audit.AuditVerifier;
import com.imgltd.mmpay.audit.AuditWriter;
import com.imgltd.mmpay.audit.JdbcAuditEventStore;
import com.imgltd.mmpay.audit.JdbcAuditEventStoreConfig;
import com.imgltd.mmpay.audit.PostgresAuditLock;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class AuditConfiguration {
  private static final String AUDIT_KEY_ENV = "MMPAY_AUDIT_HMAC_KEY";
  private static final int MIN_KEY_BYTES = 32;

  @Bean
  @ConditionalOnMissingBean
  AuditHasher auditHasher() {
    return new AuditHasher(auditKey());
  }

  @Bean
  @ConditionalOnMissingBean
  AuditChain auditChain(AuditHasher auditHasher) {
    return new AuditChain(auditHasher, Clock.systemUTC());
  }

  @Bean
  @ConditionalOnMissingBean
  AuditLock auditLock(JdbcTemplate jdbcTemplate) {
    return new PostgresAuditLock(jdbcTemplate);
  }

  @Bean
  @ConditionalOnMissingBean
  AuditEventStore auditEventStore(JdbcTemplate jdbcTemplate, AuditHasher auditHasher, AuditLock auditLock) {
    return new JdbcAuditEventStore(
        new JdbcAuditEventStoreConfig(jdbcTemplate, auditHasher, auditLock, Clock.systemUTC()));
  }

  @Bean
  AuditWriter auditWriter(AuditEventStore eventStore) {
    return new AuditWriter(eventStore);
  }

  @Bean
  AuditVerifier auditVerifier(AuditEventStore eventStore, AuditHasher auditHasher) {
    return new AuditVerifier(eventStore, auditHasher);
  }

  @Bean
  ApplicationRunner auditStartupHeartbeat(AuditWriter auditWriter) {
    return args ->
        auditWriter.emit(
            "system", null, "system.start", "runtime", "mmpay", Map.of("version", resolveVersion()));
  }

  static String resolveVersion() {
    var sha = System.getenv("MMPAY_GIT_COMMIT_SHA");
    if (sha != null && !sha.isBlank()) {
      return sha;
    }
    var packageVersion = AuditConfiguration.class.getPackage().getImplementationVersion();
    if (packageVersion != null && !packageVersion.isBlank()) {
      return packageVersion;
    }
    return "unknown";
  }

  private static byte[] auditKey() {
    var encoded = System.getenv(AUDIT_KEY_ENV);
    if (encoded == null || encoded.isBlank()) {
      throw new IllegalStateException("audit_key_missing");
    }
    var decoded = decodeAuditKey(encoded);
    if (decoded.length < MIN_KEY_BYTES) {
      throw new IllegalStateException("audit_key_too_short");
    }
    return decoded;
  }

  private static byte[] decodeAuditKey(String encoded) {
    try {
      return Base64.getDecoder().decode(encoded);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("audit_key_invalid", exception);
    }
  }
}
