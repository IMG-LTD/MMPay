package com.imgltd.mmpay.payment;

import com.imgltd.mmpay.audit.AuditWriter;
import com.imgltd.mmpay.credentials.EnvironmentReferenceResolver;
import com.imgltd.mmpay.credentials.ReferenceResolutionException;
import com.imgltd.mmpay.merchant.AdminProblemException;
import com.imgltd.mmpay.merchant.ListResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

public class PaymentService {
  private static final String ACCEPTED = "accepted";
  private static final String REJECTED = "rejected";
  private static final int BULK_MAX_EVENTS = 10000;
  private static final int DEFAULT_RPS = 10;
  private final PaymentRepository repository;
  private final EnvironmentReferenceResolver resolver;
  private final AuditWriter auditWriter;
  private final Clock clock;
  private final boolean providerLiveCalls;

  public PaymentService(
      PaymentRepository repository,
      EnvironmentReferenceResolver resolver,
      AuditWriter auditWriter,
      Clock clock,
      @Value("${mmpay.provider-live-calls:false}") boolean providerLiveCalls) {
    this.repository = repository;
    this.resolver = resolver;
    this.auditWriter = auditWriter;
    this.clock = clock;
    this.providerLiveCalls = providerLiveCalls;
  }

  public PaymentIntentResponse createIntent(PaymentIntentCreateRequest request, String idempotencyKey, String actor) {
    validateIntentCreate(request, idempotencyKey);
    ChannelCredentialRow channel = repository.requireActiveChannel(request.channelId());
    requireCurrentChannelCredential(channel, actor);
    if (!providerLiveCalls) {
      audit(actor, "payment_intent.create", "payment_intent", request.orderRef(), rejected("provider_live_disabled"));
      throw PaymentProblems.serviceUnavailable(
          PaymentProblems.PROVIDER_LIVE_DISABLED, "provider live calls are disabled");
    }
    audit(actor, "payment_intent.create", "payment_intent", request.orderRef(), rejected("provider_client_unwired"));
    throw PaymentProblems.serviceUnavailable(
        PaymentProblems.PROVIDER_LIVE_DISABLED, "live provider client is not wired");
  }

  public ListResponse<PaymentIntentResponse> listPaymentIntents() {
    return new ListResponse<>(repository.listPaymentIntents().stream().map(PaymentIntentResponse::from).toList(), null, false);
  }

  public PaymentIntentResponse getPaymentIntent(String id) {
    return PaymentIntentResponse.from(repository.requirePaymentIntent(id));
  }

  public PaymentIntentResponse cancelPaymentIntent(String id, String actor) {
    var row = repository.cancelPendingIntent(id, Instant.now(clock));
    audit(actor, "payment_intent.cancel", "payment_intent", id, accepted());
    return PaymentIntentResponse.from(row);
  }

  @Transactional
  public PaymentIntentResponse acceptCallback(
      String providerCode, ProviderCallbackRequest request, String signatureSha, String actor) {
    boolean inserted = repository.insertProviderEvent(providerCode, request, signatureSha, Instant.now(clock));
    if (!inserted) {
      return getPaymentIntent(request.paymentIntentId());
    }
    try {
      var row = repository.applyProviderState(request, Instant.now(clock));
      audit(actor, "webhook_in.callback_accepted", "payment_intent", row.id(), accepted());
      audit(actor, "transaction.observed", "payment_intent", row.id(), accepted());
      return PaymentIntentResponse.from(row);
    } catch (RuntimeException exception) {
      audit(actor, "payment_intent.state_inconsistent", "payment_intent", request.paymentIntentId(), rejected("race_lost"));
      throw exception;
    }
  }

  public RefundResponse createRefund(RefundCreateRequest request, String actor) {
    validateRefundCreate(request);
    var intent = repository.requirePaymentIntent(request.paymentIntentId());
    requireRefundable(intent);
    if (repository.refundedAmount(intent.id()) + request.amountMinor() > intent.amountMinor()) {
      audit(actor, "refund.create", "refund", intent.id(), rejected("refund_exceeds_intent"));
      throw PaymentProblems.unprocessable(PaymentProblems.REFUND_EXCEEDS_INTENT, "refund total exceeds intent amount");
    }
    audit(actor, "refund.create", "refund", intent.id(), rejected("provider_live_disabled"));
    throw PaymentProblems.serviceUnavailable(PaymentProblems.PROVIDER_LIVE_DISABLED, "provider live calls are disabled");
  }

  public ListResponse<RefundResponse> listRefunds() {
    return new ListResponse<>(repository.listRefunds().stream().map(RefundResponse::from).toList(), null, false);
  }

  public RefundResponse getRefund(String id) {
    return RefundResponse.from(repository.requireRefund(id));
  }

  public RefundResponse cancelRefund(String id, String actor) {
    try {
      var row = repository.cancelPendingRefund(id, Instant.now(clock));
      audit(actor, "refund.cancel", "refund", id, accepted());
      return RefundResponse.from(row);
    } catch (RuntimeException exception) {
      audit(actor, "refund.cancel", "refund", id, rejected("state_transition_illegal"));
      throw exception;
    }
  }

  public ListResponse<ReconciliationRunResponse> listReconciliationRuns() {
    var items = repository.listReconciliationRuns().stream().map(ReconciliationRunResponse::from).toList();
    return new ListResponse<>(items, null, false);
  }

  public ReconciliationRunResponse ackReconciliationRun(long id, String actor) {
    var row = repository.ackReconciliationRun(id, actor, Instant.now(clock));
    audit(actor, "reconciliation.ack", "reconciliation_run", String.valueOf(id), accepted());
    return ReconciliationRunResponse.from(row);
  }

  public WebhookIntegrationResponse createWebhookIntegration(WebhookIntegrationCreateRequest request, String actor) {
    new TargetUrlValidator().requireAcceptable(request.targetUrl());
    String fingerprint = resolveFingerprint(request.secretRef());
    var row = repository.insertWebhookIntegration(request, fingerprint, Instant.now(clock));
    audit(actor, "webhook_out.integration_create", "webhook_integration", row.id(), accepted());
    return WebhookIntegrationResponse.from(row);
  }

  public BulkRedispatchResponse bulkRedispatch(BulkRedispatchRequest request, String actor) {
    int rps = request.rps() == null ? DEFAULT_RPS : request.rps();
    if (request.eventCount() > BULK_MAX_EVENTS) {
      audit(actor, "webhook_out.bulk_redispatch", "webhook_integration", request.integrationId(), rejected("too_large"));
      throw PaymentProblems.unprocessable(PaymentProblems.BULK_TOO_LARGE, "bulk redispatch exceeds max events");
    }
    var now = Instant.now(clock);
    // Look back 7 days by default; spec §5.3 lets the operator supply a time range, but for the
    // current admin surface (single-integration ack), use a fixed window. P5 spec defers
    // operator-supplied range to v1.x.
    var from = now.minus(java.time.Duration.ofDays(7));
    var batchId = java.util.UUID.randomUUID();
    int inserted;
    try {
      inserted =
          repository.bulkRedispatchDeadLetters(request.integrationId(), from, now, request.eventCount(), now, batchId);
    } catch (AdminProblemException exception) {
      String reason =
          PaymentProblems.STATE_TRANSITION_ILLEGAL.equals(exception.type())
                  && "bulk_redispatch_in_flight".equals(exception.getMessage())
              ? "in_flight"
              : "rejected";
      audit(actor, "webhook_out.bulk_redispatch", "webhook_integration", request.integrationId(), rejected(reason));
      throw exception;
    } catch (RuntimeException exception) {
      audit(actor, "webhook_out.bulk_redispatch", "webhook_integration", request.integrationId(), rejected("internal_error"));
      throw exception;
    }
    int drainSeconds = (int) Math.ceil((double) Math.max(1, inserted) / Math.max(1, rps));
    audit(
        actor,
        "webhook_out.bulk_redispatch",
        "webhook_integration",
        request.integrationId(),
        details("result", ACCEPTED, "inserted", inserted, "batch_id", batchId.toString(), "rps", rps));
    return new BulkRedispatchResponse(request.integrationId(), inserted, rps, drainSeconds);
  }

  public ListResponse<DeliveryLogResponse> listDeliveryLogs() {
    var items = repository.listDeliveryLogs().stream().map(DeliveryLogResponse::from).toList();
    return new ListResponse<>(items, null, false);
  }

  public DeliveryLogResponse getDeliveryLog(long id) {
    return DeliveryLogResponse.from(repository.requireDeliveryLog(id));
  }

  public DeliveryLogResponse redispatchDeliveryLog(long id, String actor) {
    var row = repository.redispatchDeliveryLog(id, Instant.now(clock));
    audit(actor, "webhook_out.delivery_attempt", "delivery_log", String.valueOf(id), accepted());
    return DeliveryLogResponse.from(row);
  }

  private void requireCurrentChannelCredential(ChannelCredentialRow channel, String actor) {
    String resolved = resolveFingerprint(channel.credentialRef());
    if (!resolved.equals(channel.credentialFingerprint())) {
      audit(actor, "credential_ref.mismatch_detected", "channel", channel.id(), rejected("fingerprint_rebind_required"));
      throw PaymentProblems.conflict(
          PaymentProblems.FINGERPRINT_REBIND_REQUIRED, "channel credential fingerprint is stale");
    }
  }

  private String resolveFingerprint(String ref) {
    try {
      return resolver.resolve(ref).fingerprint8();
    } catch (ReferenceResolutionException exception) {
      throw PaymentProblems.unprocessable("urn:mmpay:problem:credential-ref-invalid", exception.failure().name().toLowerCase());
    }
  }

  private void requireRefundable(PaymentIntentRow intent) {
    if (!"succeeded".equals(intent.status()) && !"partially_refunded".equals(intent.status())) {
      throw PaymentProblems.conflict(PaymentProblems.REFUND_ON_NON_SUCCEEDED, "refund requires succeeded intent");
    }
  }

  private void validateIntentCreate(PaymentIntentCreateRequest request, String idempotencyKey) {
    PaymentInput.requireText(idempotencyKey, "Idempotency-Key");
    PaymentInput.requireText(request.channelId(), "channel_id");
    PaymentInput.requireAmount(request.amountMinor());
    PaymentInput.requireText(request.currency(), "currency");
    PaymentInput.requireText(request.orderRef(), "order_ref");
  }

  private void validateRefundCreate(RefundCreateRequest request) {
    PaymentInput.requireText(request.paymentIntentId(), "payment_intent_id");
    PaymentInput.requireAmount(request.amountMinor());
  }

  private void audit(String actor, String action, String kind, String id, Map<String, ?> details) {
    auditWriter.emit("user", actor, action, kind, id, details);
  }

  private Map<String, Object> accepted() {
    return details("result", ACCEPTED);
  }

  private Map<String, Object> rejected(String reason) {
    return details("result", REJECTED, "reason", reason);
  }

  private Map<String, Object> details(Object... entries) {
    var map = new LinkedHashMap<String, Object>();
    for (int index = 0; index < entries.length; index += 2) {
      map.put(entries[index].toString(), entries[index + 1]);
    }
    return map;
  }
}
