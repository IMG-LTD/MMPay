package com.imgltd.mmpay.setup;

public interface SetupAdminRepository {
  boolean adminExists();

  void createAdmin(BootstrapAdminRecord admin);
}
