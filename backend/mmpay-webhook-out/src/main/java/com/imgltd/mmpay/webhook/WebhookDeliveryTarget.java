package com.imgltd.mmpay.webhook;

@FunctionalInterface
public interface WebhookDeliveryTarget {
  WebhookTargetResponse deliver(WebhookMessage message);
}
