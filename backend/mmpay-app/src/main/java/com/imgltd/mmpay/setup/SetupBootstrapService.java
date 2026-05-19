package com.imgltd.mmpay.setup;

import com.imgltd.mmpay.audit.AuditWriter;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupBootstrapService {
  private final SetupAdminFactory adminFactory;
  private final AuditWriter auditWriter;
  private final SetupBootstrapStore store;

  public SetupBootstrapService(SetupAdminFactory adminFactory, AuditWriter auditWriter, SetupBootstrapStore store) {
    this.adminFactory = adminFactory;
    this.auditWriter = auditWriter;
    this.store = store;
  }

  @Transactional
  public String createAdmin(SetupRequest request) {
    var admin = store.createAdmin(() -> adminFactory.create(request));
    auditWriter.emit("system", null, "iam.bootstrap.complete", "user", admin.id(), Map.of("result", "accepted"));
    return admin.id();
  }
}
