package com.imgltd.mmpay.audit;

import java.util.function.Supplier;

public interface AuditLock {
  <T> T withLock(Supplier<T> operation);
}
