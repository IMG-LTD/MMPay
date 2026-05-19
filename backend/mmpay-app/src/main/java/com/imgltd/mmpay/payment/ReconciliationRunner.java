package com.imgltd.mmpay.payment;

import com.imgltd.mmpay.audit.AuditWriter;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Daily reconciliation runner. Spec P3 §1.1.4: pg_advisory_lock(reconciliation_singleton) gates
 * the run so multi-replica deploys do not produce duplicate {@code reconciliation_runs} rows.
 *
 * <p>v1.0.0 wires the cron tick + lock; the actual provider settlement-pull is provider-side and
 * lives in adapter code (out of scope for P3). This class only emits an empty-day
 * {@code reconciliation_runs} row and a {@code reconciliation.run} audit event so absence of a
 * row becomes a Prometheus alert signal per spec §7.
 */
public final class ReconciliationRunner {
  static final int RECONCILIATION_SINGLETON_LOCK = 7342;

  private static final Logger LOGGER = LoggerFactory.getLogger(ReconciliationRunner.class);

  private final JdbcTemplate jdbcTemplate;
  private final AuditWriter auditWriter;
  private final Clock clock;

  public ReconciliationRunner(JdbcTemplate jdbcTemplate, AuditWriter auditWriter, Clock clock) {
    this.jdbcTemplate = jdbcTemplate;
    this.auditWriter = auditWriter;
    this.clock = clock;
  }

  /**
   * 02:30 UTC daily by default; cron honors `mmpay.reconciliation.cron` for overrides. The cron
   * runs in UTC explicitly to match the spec acceptance criterion (off-by-one timezone drift was
   * called out in the P3 debate).
   */
  @Scheduled(cron = "${mmpay.reconciliation.cron:0 30 2 * * *}", zone = "UTC")
  public void runDaily() {
    LOGGER.info("reconciliation runner tick");
    try {
      runWithSingleton();
    } catch (RuntimeException exception) {
      LOGGER.warn("reconciliation singleton run failed: {}", exception.getMessage());
    }
  }

  void runWithSingleton() {
    Boolean acquired =
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_lock(?)", Boolean.class, RECONCILIATION_SINGLETON_LOCK);
    if (Boolean.FALSE.equals(acquired)) {
      LOGGER.info("reconciliation_singleton_lost — another replica holds the lock");
      return;
    }
    try {
      var runDate = LocalDate.now(clock.withZone(ZoneOffset.UTC));
      writeEmptyDayRow(runDate, Instant.now(clock));
      emitAudit(runDate);
    } finally {
      jdbcTemplate.queryForObject(
          "SELECT pg_advisory_unlock(?)", Boolean.class, RECONCILIATION_SINGLETON_LOCK);
    }
  }

  private void writeEmptyDayRow(LocalDate runDate, Instant now) {
    int inserted =
        jdbcTemplate.update(
            "INSERT INTO reconciliation_runs (run_date, provider_code, channel_id, ingest_count, "
                + "matched_count, unmatched_count, outcome, ack_status, tenant_id) "
                + "VALUES (?, ?, ?, 0, 0, 0, 'silent', 'pending', 'default') "
                + "ON CONFLICT (run_date, provider_code, channel_id, tenant_id) DO NOTHING",
            java.sql.Date.valueOf(runDate),
            "huifu",
            "system");
    if (inserted == 0) {
      LOGGER.info("reconciliation row already exists for {}", runDate);
    }
  }

  private void emitAudit(LocalDate runDate) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("run_date", runDate.toString());
    details.put("outcome", "silent");
    auditWriter.emit(
        "system", null, "reconciliation.run", "reconciliation_run", runDate.toString(), details);
  }
}
