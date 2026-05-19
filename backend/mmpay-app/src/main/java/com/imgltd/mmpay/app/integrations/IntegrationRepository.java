package com.imgltd.mmpay.app.integrations;

import com.imgltd.mmpay.license.LicenseRelayReceipt;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class IntegrationRepository {
  private static final String TENANT_ID = "default";
  private static final int DEFAULT_LIMIT = 100;
  private final JdbcTemplate jdbcTemplate;

  public IntegrationRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  IntegrationRow createRelayIntegration(IntegrationCreateRequest request, String fingerprint, Instant now) {
    insertRelayTarget(request, now);
    jdbcTemplate.update(
        "INSERT INTO webhook_integrations (id, display_name, target_url, secret_ref, secret_fingerprint, "
            + "legacy_header_alias, legacy_module_required, status, kind, slug, relay_target_id, tenant_id, "
            + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, false, false, 'active', 'relay', ?, ?, ?, ?, ?)",
        request.id(),
        request.name(),
        request.targetUrl(),
        request.secretRef(),
        fingerprint,
        request.slug(),
        request.id(),
        TENANT_ID,
        Timestamp.from(now),
        Timestamp.from(now));
    return requireIntegration(request.id());
  }

  List<IntegrationRow> listIntegrations() {
    return jdbcTemplate.query(
        "SELECT * FROM webhook_integrations WHERE tenant_id = ? ORDER BY created_at DESC, id ASC LIMIT ?",
        this::integration,
        TENANT_ID,
        DEFAULT_LIMIT);
  }

  IntegrationRow requireIntegration(String id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM webhook_integrations WHERE tenant_id = ? AND id = ?", this::integration, TENANT_ID, id);
    } catch (EmptyResultDataAccessException exception) {
      throw IntegrationInput.problem("urn:mmpay:problem:integration-not-found", "integration not found");
    }
  }

  RelayTargetRow requireActiveRelayTarget(String id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT id, target_url, status FROM license_relay_targets "
              + "WHERE tenant_id = ? AND id = ? AND status = 'active'",
          this::relayTarget,
          TENANT_ID,
          id);
    } catch (EmptyResultDataAccessException exception) {
      throw IntegrationInput.problem("urn:mmpay:problem:relay-target-not-active", "relay target is not active");
    }
  }

  LicenseRelayLogRow insertForwardLog(LicenseRelayReceipt receipt, boolean synthetic, Instant now) {
    long id =
        insertLog(
            new LogInsert(
                receipt.targetId(),
                receipt.requestId(),
                1,
                receipt.byteCount(),
                receipt.payloadSha256(),
                receipt.httpStatus(),
                receipt.responseSha256(),
                receipt.responseSizeBytes(),
                receipt.responseTruncated(),
                receipt.errorClass(),
                synthetic,
                receipt.errorClass() != null,
                now));
    return requireLog(id);
  }

  LicenseRelayLogRow insertSyntheticLog(String targetId, String payloadSha256, Instant now) {
    long id =
        insertLog(
            new LogInsert(targetId, "synthetic-test", 1, 0, payloadSha256, 202, null, 0, false, null, true, false, now));
    return requireLog(id);
  }

  LicenseRelayLogRow redispatchSyntheticLog(long id, Instant now) {
    var source = requireLog(id);
    if (!source.synthetic()) {
      throw IntegrationInput.problem("urn:mmpay:problem:relay-payload-not-retained", "relay payload is not retained");
    }
    long nextId =
        insertLog(
            new LogInsert(
                source.targetId(),
                source.requestId(),
                source.attempt() + 1,
                source.byteCount(),
                source.payloadSha256(),
                source.httpStatus(),
                source.responseSha256(),
                source.responseSizeBytes(),
                source.responseTruncated(),
                source.errorClass(),
                true,
                source.deadLetter(),
                now));
    return requireLog(nextId);
  }

  long successfulForwardCount() {
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM license_relay_logs WHERE tenant_id = ? AND synthetic = false AND error_class IS NULL",
            Long.class,
            TENANT_ID);
    return count == null ? 0L : count;
  }

  private void insertRelayTarget(IntegrationCreateRequest request, Instant now) {
    jdbcTemplate.update(
        "INSERT INTO license_relay_targets (id, display_name, target_url, status, tenant_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, 'active', ?, ?, ?)",
        request.id(),
        request.name(),
        request.targetUrl(),
        TENANT_ID,
        Timestamp.from(now),
        Timestamp.from(now));
  }

  private long insertLog(LogInsert log) {
    var keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(connection -> prepareInsertLog(connection.prepareStatement(logSql(), Statement.RETURN_GENERATED_KEYS), log), keyHolder);
    Number key = keyHolder.getKey();
    if (key == null) {
      throw new IllegalStateException("license_relay_log_id_missing");
    }
    return key.longValue();
  }

  private PreparedStatement prepareInsertLog(PreparedStatement statement, LogInsert log) throws SQLException {
    statement.setString(1, log.targetId());
    statement.setString(2, log.requestId());
    statement.setInt(3, log.attempt());
    statement.setInt(4, log.byteCount());
    statement.setString(5, log.payloadSha256());
    statement.setObject(6, log.httpStatus());
    statement.setString(7, log.responseSha256());
    statement.setObject(8, log.responseSizeBytes());
    statement.setBoolean(9, log.responseTruncated());
    statement.setString(10, log.errorClass());
    statement.setBoolean(11, log.synthetic());
    statement.setBoolean(12, log.deadLetter());
    statement.setTimestamp(13, Timestamp.from(log.dispatchedAt()));
    statement.setString(14, TENANT_ID);
    return statement;
  }

  private String logSql() {
    return "INSERT INTO license_relay_logs (target_id, request_id, attempt, byte_count, payload_sha256, "
        + "http_status, response_sha256, response_size_bytes, response_truncated, error_class, synthetic, "
        + "dead_letter, dispatched_at, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  }

  private LicenseRelayLogRow requireLog(long id) {
    return jdbcTemplate.queryForObject(
        "SELECT * FROM license_relay_logs WHERE tenant_id = ? AND id = ?", this::relayLog, TENANT_ID, id);
  }

  private IntegrationRow integration(ResultSet rs, int rowNum) throws SQLException {
    return new IntegrationRow(
        rs.getString("id"),
        rs.getString("kind"),
        rs.getString("display_name"),
        rs.getString("slug"),
        rs.getString("target_url"),
        rs.getString("status"),
        rs.getString("relay_target_id"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant());
  }

  private RelayTargetRow relayTarget(ResultSet rs, int rowNum) throws SQLException {
    return new RelayTargetRow(rs.getString("id"), rs.getString("target_url"), rs.getString("status"));
  }

  private LicenseRelayLogRow relayLog(ResultSet rs, int rowNum) throws SQLException {
    return new LicenseRelayLogRow(
        rs.getLong("id"),
        rs.getString("target_id"),
        rs.getString("request_id"),
        rs.getInt("attempt"),
        rs.getInt("byte_count"),
        rs.getString("payload_sha256"),
        integer(rs, "http_status"),
        rs.getString("response_sha256"),
        integer(rs, "response_size_bytes"),
        rs.getBoolean("response_truncated"),
        rs.getString("error_class"),
        rs.getBoolean("synthetic"),
        rs.getBoolean("dead_letter"),
        rs.getTimestamp("dispatched_at").toInstant());
  }

  private Integer integer(ResultSet rs, String column) throws SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private record LogInsert(
      String targetId,
      String requestId,
      int attempt,
      int byteCount,
      String payloadSha256,
      Integer httpStatus,
      String responseSha256,
      Integer responseSizeBytes,
      boolean responseTruncated,
      String errorClass,
      boolean synthetic,
      boolean deadLetter,
      Instant dispatchedAt) {}
}
