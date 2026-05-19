package com.imgltd.mmpay.system;

import org.springframework.boot.ExitCodeGenerator;

public final class RestoreModeConfigurationException extends RuntimeException implements ExitCodeGenerator {
  private final int exitCode;

  public RestoreModeConfigurationException(String message, int exitCode) {
    super(message);
    this.exitCode = exitCode;
  }

  @Override
  public int getExitCode() {
    return exitCode;
  }
}
