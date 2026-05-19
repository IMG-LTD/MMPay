package com.imgltd.mmpay.setup;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SetupStartupTokenPrinter implements ApplicationRunner {
  private final SetupAdminRepository repository;
  private final SetupTokenService tokenService;

  public SetupStartupTokenPrinter(SetupAdminRepository repository, SetupTokenService tokenService) {
    this.repository = repository;
    this.tokenService = tokenService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (!tokenService.setupAvailable() || repository.adminExists()) {
      return;
    }
    System.out.println(tokenService.stdoutLine());
  }
}
