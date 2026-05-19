package com.imgltd.mmpay.app;

import com.imgltd.mmpay.adapter.ProviderDescriptor;
import com.imgltd.mmpay.adapter.ProviderRegistry;
import com.imgltd.mmpay.audit.AuditWriter;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import com.imgltd.mmpay.huifu.HuifuProviderDescriptor;
import com.imgltd.mmpay.merchant.CredentialBindingService;
import com.imgltd.mmpay.merchant.IdempotencyStore;
import com.imgltd.mmpay.merchant.MerchantAdminRepository;
import com.imgltd.mmpay.merchant.MerchantAdminService;
import java.time.Clock;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ProviderRegistryConfiguration {
  @Bean
  ProviderDescriptor huifuProviderDescriptor() {
    return HuifuProviderDescriptor.create();
  }

  @Bean
  ProviderRegistry providerRegistry(Set<ProviderDescriptor> descriptors) {
    return new ProviderRegistry(descriptors);
  }

  @Bean
  @ConditionalOnMissingBean
  EnvironmentReferenceResolver environmentReferenceResolver() {
    return new EnvironmentReferenceResolver(System.getenv());
  }

  @Bean
  CredentialBindingService credentialBindingService(EnvironmentReferenceResolver resolver) {
    return new CredentialBindingService(resolver);
  }

  @Bean
  MerchantAdminRepository merchantAdminRepository(JdbcTemplate jdbcTemplate) {
    return new MerchantAdminRepository(jdbcTemplate);
  }

  @Bean
  MerchantAdminService merchantAdminService(
      MerchantAdminRepository repository,
      CredentialBindingService credentials,
      ProviderRegistry providers,
      AuditWriter auditWriter) {
    return new MerchantAdminService(repository, credentials, providers, auditWriter, Clock.systemUTC());
  }

  @Bean
  IdempotencyStore idempotencyStore() {
    return new IdempotencyStore(Clock.systemUTC());
  }
}
