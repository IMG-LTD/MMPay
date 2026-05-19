package com.imgltd.mmpay.setup;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SetupBootstrapStore {
  private final SetupBootstrapLock lock;
  private final SetupAdminRepository repository;

  public SetupBootstrapStore(SetupBootstrapLock lock, SetupAdminRepository repository) {
    this.lock = lock;
    this.repository = repository;
  }

  public BootstrapAdminRecord createAdmin(Supplier<BootstrapAdminRecord> adminSource) {
    lock.acquire();
    if (repository.adminExists()) {
      throw new SetupAlreadyCompletedException();
    }
    var admin = adminSource.get();
    repository.createAdmin(admin);
    return admin;
  }
}
