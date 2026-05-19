package com.imgltd.mmpay.auditverifier;

import java.util.Map;

@FunctionalInterface
public interface RestoreAttestationVerifier {
  boolean verify(Map<String, String> details);
}
