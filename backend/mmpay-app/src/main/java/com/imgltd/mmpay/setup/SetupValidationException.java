package com.imgltd.mmpay.setup;

import java.util.Map;

public final class SetupValidationException extends RuntimeException {
  private final Map<String, String> errors;

  public SetupValidationException(Map<String, String> errors) {
    super("setup_validation_failed");
    this.errors = Map.copyOf(errors);
  }

  public Map<String, String> errors() {
    return errors;
  }
}
