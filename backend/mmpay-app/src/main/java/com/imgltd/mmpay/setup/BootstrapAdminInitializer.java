package com.imgltd.mmpay.setup;

import com.imgltd.mmpay.audit.AuditWriter;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
  private static final String DEFAULT_LOCALE = "zh-CN";
  private final AuditWriter auditWriter;
  private final BootstrapAdminProperties properties;
  private final SetupAdminRepository repository;
  private final Supplier<String> idSource;

  public BootstrapAdminInitializer(
      AuditWriter auditWriter,
      BootstrapAdminProperties properties,
      SetupAdminRepository repository,
      Supplier<String> idSource) {
    this.auditWriter = auditWriter;
    this.properties = properties;
    this.repository = repository;
    this.idSource = idSource;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    properties.validate();
    if (!properties.configured() || repository.adminExists()) {
      return;
    }
    var adminId = idSource.get();
    var admin = new BootstrapAdminRecord(adminId, properties.username(), properties.passwordHash(), DEFAULT_LOCALE);
    repository.createAdmin(admin);
    auditWriter.emit("system", null, "iam.bootstrap.complete", "user", adminId, Map.of("result", "accepted"));
  }
}
