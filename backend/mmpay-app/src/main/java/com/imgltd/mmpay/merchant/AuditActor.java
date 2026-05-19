package com.imgltd.mmpay.merchant;

import org.springframework.security.core.Authentication;

record AuditActor(String kind, String id) {
  static AuditActor from(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      return new AuditActor("user", "unknown");
    }
    return new AuditActor("user", authentication.getName());
  }
}
