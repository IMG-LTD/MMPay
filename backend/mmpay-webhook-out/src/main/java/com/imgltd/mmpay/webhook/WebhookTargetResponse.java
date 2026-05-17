package com.imgltd.mmpay.webhook;

public final class WebhookTargetResponse {
  private final boolean accepted;

  private WebhookTargetResponse(boolean accepted) {
    this.accepted = accepted;
  }

  public static WebhookTargetResponse accepted() {
    return new WebhookTargetResponse(true);
  }

  public static WebhookTargetResponse rejected() {
    return new WebhookTargetResponse(false);
  }

  public boolean isAccepted() {
    return accepted;
  }
}
