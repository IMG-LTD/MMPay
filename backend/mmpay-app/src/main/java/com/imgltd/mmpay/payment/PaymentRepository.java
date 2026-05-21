package com.imgltd.mmpay.payment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public final class PaymentRepository {
  private static final String TENANT_ID = "default";
  private static final int DEFAULT_LIMIT = 100;
  private final JdbcTemplate jdbcTemplate;

  public PaymentRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  ChannelCredentialRow requireActiveChannel(String id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT id, merchant_id, provider_code, credential_ref, credential_fingerprint FROM channels "
              + "WHERE tenant_id = ? AND id = ? AND status = 'active'",
          this::channelCredential,
          TENANT_ID,
          id);
    } catch (EmptyResultDataAccessException exception) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "channel is not active");
    }
  }

  List<PaymentIntentRow> listPaymentIntents() {
    return jdbcTemplate.query(
        "SELECT * FROM payment_intents WHERE tenant_id = ? ORDER BY created_at DESC, id ASC LIMIT ?",
        this::paymentIntent,
        TENANT_ID,
        DEFAULT_LIMIT);
  }

  PaymentIntentRow requirePaymentIntent(String id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM payment_intents WHERE tenant_id = ? AND id = ?", this::paymentIntent, TENANT_ID, id);
    } catch (EmptyResultDataAccessException exception) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "payment intent not found");
    }
  }

  PaymentIntentRow cancelPendingIntent(String id, Instant now) {
    var intent = requirePaymentIntent(id);
    if (!"pending".equals(intent.status())) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "payment intent is not pending");
    }
    // Optimistic concurrency: WHERE version = ? guards against callback racing past cancel.
    int updated =
        jdbcTemplate.update(
            "UPDATE payment_intents SET status = 'cancelled', updated_at = ?, version = version + 1 "
                + "WHERE tenant_id = ? AND id = ? AND status = 'pending' AND version = ?",
            Timestamp.from(now),
            TENANT_ID,
            id,
            intent.version());
    if (updated == 0) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "payment intent state changed concurrently");
    }
    return requirePaymentIntent(id);
  }

  RefundRow cancelPendingRefund(String id, Instant now) {
    var refund = requireRefund(id);
    if (!"pending".equals(refund.status())) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "refund is not pending");
    }
    int updated =
        jdbcTemplate.update(
            "UPDATE refunds SET status = 'cancelled', version = version + 1 "
                + "WHERE tenant_id = ? AND id = ? AND status = 'pending'",
            TENANT_ID,
            id);
    if (updated == 0) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "refund state changed concurrently");
    }
    return requireRefund(id);
  }

  boolean insertProviderEvent(String providerCode, ProviderCallbackRequest request, String signatureSha, Instant now) {
    try {
      jdbcTemplate.update(
          "INSERT INTO provider_events (provider_code, provider_event_id, payment_intent_id, raw_signature_sha256, "
              + "accepted_at, duplicate_count, tenant_id) VALUES (?, ?, ?, ?, ?, 0, ?)",
          providerCode,
          request.providerEventId(),
          request.paymentIntentId(),
          signatureSha,
          Timestamp.from(now),
          TENANT_ID);
      return true;
    } catch (DuplicateKeyException exception) {
      incrementProviderDuplicate(providerCode, request.providerEventId());
      return false;
    }
  }

  PaymentIntentRow applyProviderState(ProviderCallbackRequest request, Instant now) {
    var current = requirePaymentIntent(request.paymentIntentId());
    if ("cancelled".equals(current.status())) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "callback arrived after cancel");
    }
    String nextStatus = providerNextStatus(request.status());
    jdbcTemplate.update(
        "UPDATE payment_intents SET status = ?, provider_order_id = ?, updated_at = ?, version = version + 1 "
            + "WHERE tenant_id = ? AND id = ?",
        nextStatus,
        request.providerEventId(),
        Timestamp.from(now),
        TENANT_ID,
        request.paymentIntentId());
    insertTransaction(request, nextStatus, current.currency(), now);
    return requirePaymentIntent(request.paymentIntentId());
  }

  PaymentIntentRow markSubmitted(String id, String providerOrderId, String qrCodeUrl, Instant now) {
    jdbcTemplate.update(
        "UPDATE payment_intents SET status = 'submitted', provider_order_id = ?, qr_code_url = ?, "
            + "updated_at = ?, version = version + 1 WHERE tenant_id = ? AND id = ? AND status = 'pending'",
        providerOrderId,
        qrCodeUrl,
        Timestamp.from(now),
        TENANT_ID,
        id);
    return requirePaymentIntent(id);
  }

  long refundedAmount(String intentId) {
    Long total =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(amount_minor), 0) FROM refunds WHERE tenant_id = ? "
                + "AND payment_intent_id = ? AND status <> 'cancelled'",
            Long.class,
            TENANT_ID,
            intentId);
    return total == null ? 0L : total;
  }

  List<RefundRow> listRefunds() {
    return jdbcTemplate.query(
        "SELECT * FROM refunds WHERE tenant_id = ? ORDER BY requested_at DESC, id ASC LIMIT ?",
        this::refund,
        TENANT_ID,
        DEFAULT_LIMIT);
  }

  RefundRow requireRefund(String id) {
    try {
      return jdbcTemplate.queryForObject("SELECT * FROM refunds WHERE tenant_id = ? AND id = ?", this::refund, TENANT_ID, id);
    } catch (EmptyResultDataAccessException exception) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "refund not found");
    }
  }

  List<ReconciliationRunRow> listReconciliationRuns() {
    return jdbcTemplate.query(
        "SELECT * FROM reconciliation_runs WHERE tenant_id = ? ORDER BY run_date DESC, id DESC LIMIT ?",
        this::reconciliationRun,
        TENANT_ID,
        DEFAULT_LIMIT);
  }

  ReconciliationRunRow ackReconciliationRun(long id, String actor, Instant now) {
    int updated =
        jdbcTemplate.update(
            "UPDATE reconciliation_runs SET ack_status = 'acked', ack_actor = ?, ack_at = ? WHERE tenant_id = ? AND id = ?",
            actor,
            Timestamp.from(now),
            TENANT_ID,
            id);
    if (updated == 0) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "reconciliation run not found");
    }
    return requireReconciliationRun(id);
  }

  WebhookIntegrationRow insertWebhookIntegration(
      WebhookIntegrationCreateRequest request, String fingerprint, Instant now) {
    jdbcTemplate.update(
        "INSERT INTO webhook_integrations (id, display_name, target_url, secret_ref, secret_fingerprint, "
            + "legacy_header_alias, legacy_module_required, status, tenant_id, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, false, 'active', ?, ?, ?)",
        request.id(),
        request.displayName(),
        request.targetUrl(),
        request.secretRef(),
        fingerprint,
        request.legacyHeaderAlias(),
        TENANT_ID,
        Timestamp.from(now),
        Timestamp.from(now));
    return requireWebhookIntegration(request.id());
  }

  List<DeliveryLogRow> listDeliveryLogs() {
    return jdbcTemplate.query(
        "SELECT * FROM delivery_logs WHERE tenant_id = ? ORDER BY scheduled_at DESC, id DESC LIMIT ?",
        this::deliveryLog,
        TENANT_ID,
        DEFAULT_LIMIT);
  }

  DeliveryLogRow requireDeliveryLog(long id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM delivery_logs WHERE tenant_id = ? AND id = ?", this::deliveryLog, TENANT_ID, id);
    } catch (EmptyResultDataAccessException exception) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "delivery log not found");
    }
  }

  DeliveryLogRow redispatchDeliveryLog(long id, Instant now) {
    var source = requireDeliveryLog(id);
    // Cancel any in-flight backoff rows for the same event_id so we don't double-fire.
    jdbcTemplate.update(
        "UPDATE delivery_logs SET dead_letter = TRUE "
            + "WHERE tenant_id = ? AND event_id = ? AND dispatched_at IS NULL AND dead_letter = FALSE",
        TENANT_ID,
        source.eventId());
    jdbcTemplate.update(
        "INSERT INTO delivery_logs (integration_id, payment_intent_id, event_id, attempt, scheduled_at, tenant_id) "
            + "VALUES (?, ?, ?, 1, ?, ?)",
        source.integrationId(),
        source.paymentIntentId(),
        source.eventId(),
        Timestamp.from(now),
        TENANT_ID);
    return newestDeliveryLog(source.eventId(), now);
  }

  /**
   * Bulk re-dispatch: spec §1.1.5 / §5.3 — acquire advisory lock per integration, snapshot rows
   * matching the filter, mark in-flight back-off as dead_letter, then insert attempt=1 rows for
   * each matched event_id. Returns the number of new rows actually inserted.
   */
  int bulkRedispatchDeadLetters(
      String integrationId, Instant from, Instant to, int maxRows, Instant now, UUID batchId) {
    int lockKey = bulkRedispatchLockKey(integrationId);
    // Try-lock for blast-radius isolation; competing bulks abort.
    Boolean acquired =
        jdbcTemplate.queryForObject("SELECT pg_try_advisory_lock(?)", Boolean.class, lockKey);
    if (Boolean.FALSE.equals(acquired)) {
      throw PaymentProblems.conflict(
          PaymentProblems.STATE_TRANSITION_ILLEGAL, "bulk_redispatch_in_flight");
    }
    try {
      var eventIds =
          jdbcTemplate.query(
              "SELECT DISTINCT event_id, integration_id, payment_intent_id FROM delivery_logs "
                  + "WHERE tenant_id = ? AND integration_id = ? AND dead_letter = TRUE "
                  + "AND scheduled_at BETWEEN ? AND ? LIMIT ?",
              (rs, n) ->
                  new BulkRedispatchTarget(
                      rs.getString("event_id"),
                      rs.getString("integration_id"),
                      rs.getString("payment_intent_id")),
              TENANT_ID,
              integrationId,
              Timestamp.from(from),
              Timestamp.from(to),
              maxRows);
      int inserted = 0;
      for (var target : eventIds) {
        jdbcTemplate.update(
            "INSERT INTO delivery_logs (integration_id, payment_intent_id, event_id, attempt, "
                + "scheduled_at, bulk_redispatch_batch_id, tenant_id) VALUES (?, ?, ?, 1, ?, ?, ?)",
            target.integrationId(),
            target.paymentIntentId(),
            target.eventId(),
            Timestamp.from(now),
            batchId,
            TENANT_ID);
        inserted++;
      }
      return inserted;
    } finally {
      jdbcTemplate.queryForObject("SELECT pg_advisory_unlock(?)", Boolean.class, lockKey);
    }
  }

  private static int bulkRedispatchLockKey(String integrationId) {
    // Deterministic 32-bit key per integration so the advisory lock id is stable.
    return Math.abs(("bulk_redispatch_" + integrationId).hashCode());
  }

  private record BulkRedispatchTarget(String eventId, String integrationId, String paymentIntentId) {}

  private ReconciliationRunRow requireReconciliationRun(long id) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT * FROM reconciliation_runs WHERE tenant_id = ? AND id = ?", this::reconciliationRun, TENANT_ID, id);
    } catch (EmptyResultDataAccessException exception) {
      throw PaymentProblems.conflict(PaymentProblems.STATE_TRANSITION_ILLEGAL, "reconciliation run not found");
    }
  }

  private WebhookIntegrationRow requireWebhookIntegration(String id) {
    return jdbcTemplate.queryForObject(
        "SELECT * FROM webhook_integrations WHERE tenant_id = ? AND id = ?", this::webhookIntegration, TENANT_ID, id);
  }

  private DeliveryLogRow newestDeliveryLog(String eventId, Instant scheduledAt) {
    return jdbcTemplate.queryForObject(
        "SELECT * FROM delivery_logs WHERE tenant_id = ? AND event_id = ? AND scheduled_at = ? ORDER BY id DESC LIMIT 1",
        this::deliveryLog,
        TENANT_ID,
        eventId,
        Timestamp.from(scheduledAt));
  }

  private void incrementProviderDuplicate(String providerCode, String eventId) {
    jdbcTemplate.update(
        "UPDATE provider_events SET duplicate_count = duplicate_count + 1 WHERE provider_code = ? AND provider_event_id = ?",
        providerCode,
        eventId);
  }

  private void insertTransaction(ProviderCallbackRequest request, String status, String currency, Instant now) {
    jdbcTemplate.update(
        "INSERT INTO transactions (id, payment_intent_id, provider_event_id, amount_minor, currency, status, "
            + "tenant_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        "txn_" + UUID.randomUUID(),
        request.paymentIntentId(),
        request.providerEventId(),
        request.amountMinor(),
        currency,
        status,
        TENANT_ID,
        Timestamp.from(now));
  }

  private String providerNextStatus(String status) {
    return "failed".equals(status) ? "failed" : "succeeded";
  }

  private ChannelCredentialRow channelCredential(ResultSet rs, int row) throws SQLException {
    return new ChannelCredentialRow(
        rs.getString("id"),
        rs.getString("merchant_id"),
        rs.getString("provider_code"),
        rs.getString("credential_ref"),
        rs.getString("credential_fingerprint"));
  }

  private PaymentIntentRow paymentIntent(ResultSet rs, int row) throws SQLException {
    return new PaymentIntentRow(
        rs.getString("id"),
        rs.getString("merchant_id"),
        rs.getString("channel_id"),
        rs.getString("provider_order_id"),
        rs.getString("qr_code_url"),
        rs.getLong("amount_minor"),
        rs.getString("currency"),
        rs.getString("order_ref"),
        rs.getString("status"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"),
        rs.getLong("version"));
  }

  private RefundRow refund(ResultSet rs, int row) throws SQLException {
    return new RefundRow(
        rs.getString("id"),
        rs.getString("payment_intent_id"),
        rs.getString("merchant_id"),
        rs.getLong("amount_minor"),
        rs.getString("currency"),
        rs.getString("status"),
        instant(rs, "requested_at"));
  }

  private ReconciliationRunRow reconciliationRun(ResultSet rs, int row) throws SQLException {
    return new ReconciliationRunRow(
        rs.getLong("id"),
        rs.getDate("run_date").toLocalDate(),
        rs.getString("provider_code"),
        rs.getString("channel_id"),
        rs.getLong("ingest_count"),
        rs.getLong("matched_count"),
        rs.getLong("unmatched_count"),
        rs.getString("outcome"),
        rs.getString("ack_status"),
        instantOrNull(rs, "ack_at"),
        rs.getString("ack_actor"));
  }

  private WebhookIntegrationRow webhookIntegration(ResultSet rs, int row) throws SQLException {
    return new WebhookIntegrationRow(
        rs.getString("id"),
        rs.getString("display_name"),
        rs.getString("target_url"),
        rs.getString("secret_ref"),
        rs.getString("secret_fingerprint"),
        rs.getString("status"),
        instant(rs, "created_at"),
        instant(rs, "updated_at"));
  }

  private DeliveryLogRow deliveryLog(ResultSet rs, int row) throws SQLException {
    return new DeliveryLogRow(
        rs.getLong("id"),
        rs.getString("integration_id"),
        rs.getString("payment_intent_id"),
        rs.getString("event_id"),
        rs.getInt("attempt"),
        instant(rs, "scheduled_at"),
        instantOrNull(rs, "dispatched_at"),
        nullableInt(rs, "response_status"),
        instantOrNull(rs, "next_retry_at"),
        rs.getBoolean("dead_letter"));
  }

  private Instant instant(ResultSet rs, String name) throws SQLException {
    return rs.getTimestamp(name).toInstant();
  }

  private Instant instantOrNull(ResultSet rs, String name) throws SQLException {
    Timestamp timestamp = rs.getTimestamp(name);
    return timestamp == null ? null : timestamp.toInstant();
  }

  private Integer nullableInt(ResultSet rs, String name) throws SQLException {
    int value = rs.getInt(name);
    return rs.wasNull() ? null : value;
  }
}
