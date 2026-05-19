package com.imgltd.mmpay.payment;

import com.imgltd.mmpay.audit.AuditWriter;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class PaymentConfiguration {
  @Bean
  @ConditionalOnMissingBean
  Clock mmpayClock() {
    return Clock.systemUTC();
  }

  @Bean
  PaymentRepository paymentRepository(JdbcTemplate jdbcTemplate) {
    return new PaymentRepository(jdbcTemplate);
  }

  @Bean
  PaymentService paymentService(
      PaymentRepository repository,
      EnvironmentReferenceResolver resolver,
      AuditWriter auditWriter,
      Clock clock,
      org.springframework.core.env.Environment env) {
    boolean liveCalls = env.getProperty("mmpay.provider-live-calls", Boolean.class, false);
    return new PaymentService(repository, resolver, auditWriter, clock, liveCalls);
  }

  @Bean
  ProviderCallbackVerifier providerCallbackVerifier(org.springframework.core.env.Environment env, Clock clock) {
    return new ProviderCallbackVerifier(env.getProperty("mmpay.provider-callback-secret", ""), clock);
  }
}
