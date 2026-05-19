package com.imgltd.mmpay.audit;

public class AuditDetailsLeakException extends RuntimeException {
  public AuditDetailsLeakException(String key) {
    super("audit details key is forbidden: " + key);
  }
}
