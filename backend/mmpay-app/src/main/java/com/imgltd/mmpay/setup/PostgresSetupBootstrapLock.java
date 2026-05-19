package com.imgltd.mmpay.setup;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class PostgresSetupBootstrapLock implements SetupBootstrapLock {
  static final long IAM_BOOTSTRAP_LOCK_ID = 7340L;
  private final JdbcTemplate jdbcTemplate;

  public PostgresSetupBootstrapLock(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void acquire() {
    jdbcTemplate.query(
        "SELECT pg_advisory_xact_lock(?)",
        statement -> statement.setLong(1, IAM_BOOTSTRAP_LOCK_ID),
        resultSet -> null);
  }
}
