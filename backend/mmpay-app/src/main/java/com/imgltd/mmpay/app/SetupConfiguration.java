package com.imgltd.mmpay.app;

import com.imgltd.mmpay.setup.BootstrapAdminProperties;
import com.imgltd.mmpay.setup.MmpayActuatorSanitizer;
import com.imgltd.mmpay.setup.SetupTokenService;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.boot.actuate.endpoint.SanitizableData;
import org.springframework.boot.actuate.endpoint.SanitizingFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SetupConfiguration {
  private static final int TOKEN_BYTES = 32;

  @Bean
  BootstrapAdminProperties bootstrapAdminProperties() {
    return new BootstrapAdminProperties(
        System.getenv("MMPAY_BOOTSTRAP_ADMIN_USERNAME"), System.getenv("MMPAY_BOOTSTRAP_ADMIN_PASSWORD_HASH"));
  }

  @Bean
  @ConditionalOnMissingBean
  SetupTokenService setupTokenService(BootstrapAdminProperties properties) {
    return SetupTokenService.create(false, properties, SetupConfiguration::randomTokenBytes);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  Supplier<String> setupAdminIdSource() {
    return () -> UUID.randomUUID().toString();
  }

  @Bean
  Clock setupClock() {
    return Clock.systemUTC();
  }

  @Bean
  MmpayActuatorSanitizer mmpayActuatorSanitizer() {
    return new MmpayActuatorSanitizer();
  }

  @Bean
  SanitizingFunction mmpaySanitizingFunction(MmpayActuatorSanitizer sanitizer) {
    return data -> sanitize(data, sanitizer);
  }

  private static byte[] randomTokenBytes() {
    var bytes = new byte[TOKEN_BYTES];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  private static SanitizableData sanitize(SanitizableData data, MmpayActuatorSanitizer sanitizer) {
    var value = data.getValue();
    var sanitized = sanitizer.sanitize(data.getKey(), value == null ? null : value.toString());
    return sanitized == null ? data.withValue(null) : data.withValue(sanitized);
  }
}
