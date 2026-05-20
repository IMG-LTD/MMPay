package com.imgltd.mmpay.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.transaction.annotation.Transactional;

public class JdbcAuditEventStore implements AuditEventStore {
  private static final TypeReference<Map<String, Object>> DETAILS_TYPE = new TypeReference<>() {};
  private final AuditHasher hasher;
  private final JdbcTemplate jdbcTemplate;
  private final AuditLock lock;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Clock clock;

  public JdbcAuditEventStore(JdbcAuditEventStoreConfig config) {
    hasher = config.hasher();
    jdbcTemplate = config.jdbcTemplate();
    lock = config.lock();
    clock = config.clock();
  }

  @Override
  @Transactional
  public AuditEvent append(AuditAppendRequest request) {
    return lock.withLock(() -> appendLocked(request));
  }

  private AuditEvent appendLocked(AuditAppendRequest request) {
    AuditDetails.validate(request.details());
    var previous = previousHmac();
    var timestamp = Instant.now(clock);
    var event = newEvent(request, previous, nextId(), timestamp);
    insert(event, detailsJson(event.details()));
    return event;
  }

  @Override
  public List<AuditEvent> events() {
    return jdbcTemplate.query("SELECT * FROM audit_event ORDER BY id", (resultSet, rowNumber) -> event(resultSet));
  }

  private AuditEvent newEvent(AuditAppendRequest request, String previous, long id, Instant timestamp) {
    var draft =
        new AuditEvent(
            id,
            timestamp,
            request.actorKind(),
            request.actorId(),
            request.action(),
            request.targetKind(),
            request.targetId(),
            Map.copyOf(request.details()),
            previous,
            "");
    return draft.withRowHmac(hasher.rowHmac(draft, previous));
  }

  private void insert(AuditEvent event, String detailsJson) {
    jdbcTemplate.update(
        "INSERT INTO audit_event (id, ts, actor_kind, actor_id, action, target_kind, target_id, details_json, "
            + "prev_row_hmac, row_hmac, tenant_id, chain_anchor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'default', FALSE)",
        event.id(),
        Timestamp.from(event.timestamp()),
        event.actorKind(),
        event.actorId(),
        event.action(),
        event.targetKind(),
        event.targetId(),
        new SqlParameterValue(Types.OTHER, detailsJson),
        event.prevRowHmac(),
        event.rowHmac());
  }

  private String previousHmac() {
    var rows = jdbcTemplate.queryForList("SELECT row_hmac FROM audit_event ORDER BY id DESC LIMIT 1", String.class);
    return rows.isEmpty() ? null : rows.getFirst();
  }

  private long nextId() {
    Long id = jdbcTemplate.queryForObject("SELECT nextval('audit_event_id_seq')", Long.class);
    return id == null ? 0L : id;
  }

  private AuditEvent event(ResultSet resultSet) throws SQLException {
    return new AuditEvent(
        resultSet.getLong("id"),
        resultSet.getTimestamp("ts").toInstant(),
        resultSet.getString("actor_kind"),
        resultSet.getString("actor_id"),
        resultSet.getString("action"),
        resultSet.getString("target_kind"),
        resultSet.getString("target_id"),
        details(resultSet.getString("details_json")),
        resultSet.getString("prev_row_hmac"),
        resultSet.getString("row_hmac"));
  }

  private String detailsJson(Map<String, ?> details) {
    try {
      return objectMapper.writeValueAsString(details);
    } catch (Exception exception) {
      throw new IllegalStateException("audit_details_json_failed", exception);
    }
  }

  private Map<String, ?> details(String detailsJson) {
    try {
      return objectMapper.readValue(detailsJson, DETAILS_TYPE);
    } catch (Exception exception) {
      throw new IllegalStateException("audit_details_parse_failed", exception);
    }
  }
}
