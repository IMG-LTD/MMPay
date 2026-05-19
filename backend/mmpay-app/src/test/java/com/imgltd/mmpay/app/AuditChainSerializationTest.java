package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgltd.mmpay.audit.AuditAppendRequest;
import com.imgltd.mmpay.audit.AuditEvent;
import com.imgltd.mmpay.audit.AuditHasher;
import com.imgltd.mmpay.audit.AuditLock;
import com.imgltd.mmpay.audit.JdbcAuditEventStore;
import com.imgltd.mmpay.audit.JdbcAuditEventStoreConfig;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AuditChainSerializationTest {
  private static final int WRITERS = 64;
  private static final byte[] HMAC_KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

  @Test
  void concurrentJdbcWritersProduceAMonotoneVerifiableChain() throws Exception {
    var jdbcTemplate = new JdbcTemplate(dataSource());
    createAuditSchema(jdbcTemplate);
    var hasher = new AuditHasher(HMAC_KEY);
    var store = new JdbcAuditEventStore(new JdbcAuditEventStoreConfig(jdbcTemplate, hasher, new SerialAuditLock(), clock()));

    appendConcurrently(store);

    var events = store.events();
    assertThat(events).hasSize(WRITERS);
    assertThat(events.stream().map(AuditEvent::id).toList()).containsExactlyElementsOf(expectedIds());
    assertThat(events.stream().map(AuditEvent::prevRowHmac).filter(value -> value != null).distinct()).hasSize(WRITERS - 1);
    assertThat(hasher.verify(events).ok()).isTrue();
  }

  private static void appendConcurrently(JdbcAuditEventStore store) throws Exception {
    var started = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(WRITERS)) {
      var futures = new ArrayList<java.util.concurrent.Future<?>>();
      for (int writer = 0; writer < WRITERS; writer++) {
        var writerId = writer;
        futures.add(executor.submit(() -> appendAfterStart(store, started, writerId)));
      }
      started.countDown();
      for (var future : futures) {
        future.get();
      }
    }
  }

  private static void appendAfterStart(JdbcAuditEventStore store, CountDownLatch started, int writerId) {
    try {
      started.await();
      store.append(request(writerId));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("audit_serialization_interrupted", exception);
    }
  }

  private static AuditAppendRequest request(int writerId) {
    return new AuditAppendRequest("system", null, "system.start", "runtime", "writer-" + writerId, Map.of("result", "ok"));
  }

  private static List<Long> expectedIds() {
    var ids = new ArrayList<Long>();
    for (long id = 1; id <= WRITERS; id++) {
      ids.add(id);
    }
    return ids;
  }

  private static DriverManagerDataSource dataSource() {
    return new DriverManagerDataSource("jdbc:h2:mem:audit_serialization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
  }

  private static void createAuditSchema(JdbcTemplate jdbcTemplate) {
    jdbcTemplate.execute("CREATE SEQUENCE audit_event_id_seq START WITH 1 INCREMENT BY 1");
    jdbcTemplate.execute(
        "CREATE TABLE audit_event (id BIGINT PRIMARY KEY, ts TIMESTAMP NOT NULL, actor_kind VARCHAR(16) NOT NULL, "
            + "actor_id VARCHAR(64), action VARCHAR(64) NOT NULL, target_kind VARCHAR(32) NOT NULL, "
            + "target_id VARCHAR(128), request_id VARCHAR(64), before_hash CHAR(64), after_hash CHAR(64), "
            + "details_json VARCHAR, prev_row_hmac CHAR(64), row_hmac CHAR(64) NOT NULL, tenant_id VARCHAR(32), "
            + "chain_anchor BOOLEAN)");
  }

  private static Clock clock() {
    return Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC);
  }

  private static final class SerialAuditLock implements AuditLock {
    @Override
    public synchronized <T> T withLock(Supplier<T> operation) {
      return operation.get();
    }
  }
}
