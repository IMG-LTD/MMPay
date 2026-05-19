package com.imgltd.mmpay.system;

import java.util.HashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class SystemModeConfiguration {
  @Bean
  DegradedModeGuard degradedModeGuard(@Value("${mmpay.fail-open-degraded-ui:false}") boolean degraded) {
    return new DegradedModeGuard(degraded);
  }

  @Bean
  ApplicationRunner restoreModeStartupGuard(Environment environment) {
    return args -> RestoreModeGuard.validate(restoreEnv(environment));
  }

  private static HashMap<String, String> restoreEnv(Environment environment) {
    var values = new HashMap<String, String>();
    put(values, "MMPAY_RESTORE_MODE", environment.getProperty("MMPAY_RESTORE_MODE"));
    put(values, "MMPAY_AUDIT_RESTORE_ATTESTATION_KEY", environment.getProperty("MMPAY_AUDIT_RESTORE_ATTESTATION_KEY"));
    return values;
  }

  private static void put(HashMap<String, String> values, String key, String value) {
    if (value != null) {
      values.put(key, value);
    }
  }
}
