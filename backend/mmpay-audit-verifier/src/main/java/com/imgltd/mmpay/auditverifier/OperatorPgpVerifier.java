package com.imgltd.mmpay.auditverifier;

@FunctionalInterface
public interface OperatorPgpVerifier {
  boolean verify(String detachedSignature);
}
