package com.imgltd.mmpay.webhook;

public record WebhookMessage(String body, String signatureHeader) {}
