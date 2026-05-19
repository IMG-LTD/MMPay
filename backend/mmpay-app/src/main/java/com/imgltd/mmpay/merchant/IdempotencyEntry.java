package com.imgltd.mmpay.merchant;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record IdempotencyEntry(String requestHash, HttpStatus status, Object body, Instant expiresAt) {}
