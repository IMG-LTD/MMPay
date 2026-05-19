package com.imgltd.mmpay.credentials;

public class ReferenceResolutionException extends RuntimeException {
  private final ReferenceResolutionFailure failure;

  public ReferenceResolutionException(ReferenceResolutionFailure failure, String reference) {
    super(failure + ": " + reference);
    this.failure = failure;
  }

  public ReferenceResolutionFailure failure() {
    return failure;
  }
}
