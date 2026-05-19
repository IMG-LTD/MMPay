package com.imgltd.mmpay.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.imgltd.mmpay.audit.AuditChain;
import com.imgltd.mmpay.audit.AuditDetails;
import com.imgltd.mmpay.audit.AuditDetailsLeakException;
import com.imgltd.mmpay.audit.AuditEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class AuditFoundationTest {
  private static final byte[] HMAC_KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

  @Test
  void rejectsForbiddenDetailsKeysBeforeWritingAuditRows() {
    var details = Map.of("apiKey", "not logged", "display_name", "Acme");

    assertThrows(AuditDetailsLeakException.class, () -> AuditDetails.validate(details));
    assertEquals("(?i)(password|secret|key|token|signature|hash|cert|pem|license_payload)", AuditDetails.FORBIDDEN_KEY_REGEX);
  }

  @Test
  void rejectsForbiddenDetailsKeysInNestedPayloads() {
    var forbiddenKeys = java.util.List.of("content_hash", "tls_cert", "pem_block", "license_payload");

    for (String key : forbiddenKeys) {
      var details = Map.of("metadata", Map.of(key, "not logged"));

      assertThrows(AuditDetailsLeakException.class, () -> AuditDetails.validate(details), key);
    }
  }

  @Test
  void verifiesLinearHmacChainAndDetectsTampering() {
    var chain = new AuditChain(HMAC_KEY);
    chain.emit("system", null, "system.start", "runtime", "mmpay", Map.of("version", "test"));
    AuditEvent second = chain.emit("user", "admin", "iam.bootstrap.complete", "user", "admin", Map.of("result", "accepted"));

    assertTrue(chain.verify().ok());
    assertEquals(second.id(), chain.verify().toId());
    var tampered = chain.events();
    tampered.set(1, second.withAction("tampered"));
    assertEquals(second.id(), chain.verify(tampered).firstBreakId());
  }

  @Test
  void serializesConcurrentWritersWithMonotoneIdsAndUniquePrevHmac() throws Exception {
    var chain = new AuditChain(HMAC_KEY);
    var writers = 16;
    var started = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(writers);
    var futures = new ArrayList<java.util.concurrent.Future<?>>();

    for (int i = 0; i < writers; i++) {
      futures.add(pool.submit(() -> emitAfterStart(chain, started)));
    }

    started.countDown();
    for (var future : futures) {
      future.get();
    }
    pool.shutdown();

    var events = chain.events();
    assertEquals(writers, events.size());
    assertEquals(writers, events.getLast().id());
    assertEquals(writers - 1, events.stream().map(AuditEvent::prevRowHmac).filter(value -> value != null).distinct().count());
    assertTrue(chain.verify().ok());
  }

  private static void emitAfterStart(AuditChain chain, CountDownLatch started) {
    try {
      started.await();
      chain.emit("system", null, "system.start", "runtime", "mmpay", Map.of("version", "test"));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
