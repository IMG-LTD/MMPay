package com.imgltd.mmpay.setup;

public final class SetupAlreadyCompletedException extends RuntimeException {
  public SetupAlreadyCompletedException() {
    super("setup_already_completed");
  }
}
