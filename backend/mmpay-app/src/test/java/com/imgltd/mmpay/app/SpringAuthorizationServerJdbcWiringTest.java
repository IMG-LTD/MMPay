package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

class SpringAuthorizationServerJdbcWiringTest {
  @Test
  void exposesSpringAuthorizationServerJdbcStores() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(dataSource()));
      context.register(IamAuthorizationServerConfiguration.class);

      context.refresh();

      assertThat(context.getBean(RegisteredClientRepository.class)).isInstanceOf(JdbcRegisteredClientRepository.class);
      assertThat(context.getBean(OAuth2AuthorizationService.class)).isInstanceOf(JdbcOAuth2AuthorizationService.class);
      assertThat(context.getBean(OAuth2AuthorizationConsentService.class))
          .isInstanceOf(JdbcOAuth2AuthorizationConsentService.class);
    }
  }

  private static DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource("jdbc:h2:mem:sas_wiring;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
  }
}
