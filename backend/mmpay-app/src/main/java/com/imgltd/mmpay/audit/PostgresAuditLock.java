package com.imgltd.mmpay.audit;

import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresAuditLock implements AuditLock {
  static final long AUDIT_EVENT_CHAIN_LOCK_ID = 7341L;
  private final JdbcTemplate jdbcTemplate;

  public PostgresAuditLock(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public <T> T withLock(Supplier<T> operation) {
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(?)",
        statement -> statement.setLong(1, AUDIT_EVENT_CHAIN_LOCK_ID),
        resultSet -> null);
    return operation.get();
  }
}
