package com.imgltd.mmpay.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.imgltd.mmpay.audit.AuditAppendRequest;
import com.imgltd.mmpay.audit.AuditHasher;
import com.imgltd.mmpay.audit.AuditLock;
import com.imgltd.mmpay.audit.JdbcAuditEventStore;
import com.imgltd.mmpay.audit.JdbcAuditEventStoreConfig;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class JdbcAuditEventStoreTest {
  private static final byte[] HMAC_KEY = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

  @Test
  void appendsAuditRowsWithPreviousHmacAfterTakingTheChainLock() {
    var jdbcTemplate = new JdbcTemplate(dataSource());
    createAuditSchema(jdbcTemplate);
    var lock = new CountingAuditLock();
    var hasher = new AuditHasher(HMAC_KEY);
    var store = new JdbcAuditEventStore(new JdbcAuditEventStoreConfig(jdbcTemplate, hasher, lock, fixedClock()));

    var first = store.append(request("system.start", "runtime", "mmpay"));
    var second = store.append(request("iam.bootstrap.complete", "user", "admin"));

    assertThat(first.id()).isEqualTo(1L);
    assertThat(second.id()).isEqualTo(2L);
    assertThat(second.prevRowHmac()).isEqualTo(first.rowHmac());
    assertThat(lock.acquisitions()).isEqualTo(2);
    assertThat(store.events()).hasSize(2);
    assertThat(hasher.verify(store.events()).ok()).isTrue();
  }

  private static SingleConnectionDataSource dataSource() {
    return new SingleConnectionDataSource("jdbc:h2:mem:audit_store;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "", true);
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

  private static Clock fixedClock() {
    return Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC);
  }

  private static AuditAppendRequest request(String action, String targetKind, String targetId) {
    return new AuditAppendRequest("system", null, action, targetKind, targetId, Map.of("result", "accepted"));
  }

  private static final class CountingAuditLock implements AuditLock {
    private final AtomicInteger acquisitions = new AtomicInteger();

    @Override
    public <T> T withLock(Supplier<T> operation) {
      acquisitions.incrementAndGet();
      return operation.get();
    }

    int acquisitions() {
      return acquisitions.get();
    }
  }
}
