package com.imgltd.mmpay.merchant;

import com.imgltd.mmpay.adapter.ProviderRegistry;
import com.imgltd.mmpay.audit.AuditWriter;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MerchantAdminService {
  private static final String ACCEPTED = "accepted";
  private static final String REJECTED = "rejected";
  private final MerchantAdminRepository repository;
  private final CredentialBindingService credentials;
  private final ProviderRegistry providers;
  private final AuditWriter auditWriter;
  private final Clock clock;

  public MerchantAdminService(
      MerchantAdminRepository repository,
      CredentialBindingService credentials,
      ProviderRegistry providers,
      AuditWriter auditWriter,
      Clock clock) {
    this.repository = repository;
    this.credentials = credentials;
    this.providers = providers;
    this.auditWriter = auditWriter;
    this.clock = clock;
  }

  public MerchantResponse createMerchant(MerchantCreateRequest request, AuditActor actor) {
    InputValidator.merchantCreate(request);
    var binding = bindOrAudit("merchant", request.id(), request.credentialRef(), actor);
    var row = repository.insertMerchant(request, binding, java.time.Instant.now(clock));
    audit(actor, "merchant.create", "merchant", row.id(), details("id", row.id(), "display_name", row.displayName()));
    auditBindAccepted(actor, "merchant", row.id(), null, row.credentialFingerprint());
    return MerchantResponse.from(row);
  }

  public ListResponse<MerchantResponse> listMerchants(String status, PageWindow page) {
    var rows = repository.listMerchants(normalizeStatus(status), page);
    var hasMore = rows.size() > page.limit();
    var visible = hasMore ? rows.subList(0, page.limit()) : rows;
    var items = visible.stream().map(MerchantResponse::from).toList();
    return new ListResponse<>(items, nextCursor(visible, hasMore), hasMore);
  }

  public MerchantResponse getMerchant(String id) {
    return MerchantResponse.from(repository.requireMerchant(id));
  }

  public ChannelResponse createChannel(String merchantId, ChannelCreateRequest request, AuditActor actor) {
    InputValidator.channelCreate(request);
    requireProvider(request.providerCode(), actor, request.id());
    var binding = bindOrAudit("channel", request.id(), request.credentialRef(), actor);
    var row = repository.insertChannel(merchantId, request, binding, java.time.Instant.now(clock));
    audit(actor, "channel.create", "channel", row.id(), channelDetails(row));
    auditBindAccepted(actor, "channel", row.id(), null, row.credentialFingerprint());
    return ChannelResponse.from(row);
  }

  public ListResponse<ChannelResponse> listChannels(String merchantId, String status, PageWindow page) {
    var rows = repository.listChannels(merchantId, normalizeStatus(status), page);
    var hasMore = rows.size() > page.limit();
    var visible = hasMore ? rows.subList(0, page.limit()) : rows;
    var items = visible.stream().map(ChannelResponse::from).toList();
    return new ListResponse<>(items, nextChannelCursor(visible, hasMore), hasMore);
  }

  public ChannelResponse getChannel(String id) {
    return ChannelResponse.from(repository.requireChannel(id));
  }

  private CredentialBindingResult bindOrAudit(String kind, String id, String ref, AuditActor actor) {
    try {
      return credentials.bind(ref);
    } catch (AdminProblemException exception) {
      auditRejected(actor, "credential_ref.bind", kind, id, exception.getMessage());
      throw exception;
    }
  }

  private void requireProvider(String code, AuditActor actor, String channelId) {
    try {
      providers.require(code);
    } catch (IllegalArgumentException exception) {
      var reason = exception.getMessage().contains("reserved") ? "provider_code_reserved" : "provider_code_unknown";
      auditRejected(actor, "channel.create", "channel", channelId, reason);
      throw AdminProblems.unprocessable(problemType(reason), reason);
    }
  }

  private void auditBindAccepted(AuditActor actor, String kind, String id, String oldFp, String newFp) {
    var details = details("entity_kind", kind, "entity_id", id, "result", ACCEPTED);
    if (oldFp != null) {
      details.put("old_fingerprint", oldFp);
    }
    details.put("new_fingerprint", newFp);
    audit(actor, "credential_ref.bind", kind, id, details);
  }

  private void auditRejected(AuditActor actor, String action, String targetKind, String targetId, String reason) {
    audit(actor, action, targetKind, targetId, details("result", REJECTED, "reason", reason));
  }

  private void audit(AuditActor actor, String action, String targetKind, String targetId, Map<String, ?> details) {
    auditWriter.emit(actor.kind(), actor.id(), action, targetKind, targetId, details);
  }

  private String normalizeStatus(String status) {
    return status == null || status.isBlank() ? "active" : status;
  }

  private String problemType(String reason) {
    return "provider_code_reserved".equals(reason) ? AdminProblems.PROVIDER_RESERVED : AdminProblems.PROVIDER_UNKNOWN;
  }

  private Map<String, Object> channelDetails(ChannelRow row) {
    return details("id", row.id(), "merchant_id", row.merchantId(), "provider_code", row.providerCode());
  }

  private Map<String, Object> details(Object... entries) {
    var map = new LinkedHashMap<String, Object>();
    for (var index = 0; index < entries.length; index += 2) {
      map.put(entries[index].toString(), entries[index + 1]);
    }
    return map;
  }

  private String nextCursor(java.util.List<MerchantRow> rows, boolean hasMore) {
    if (!hasMore || rows.isEmpty()) {
      return null;
    }
    var last = rows.getLast();
    return PageCursor.encode(last.createdAt(), last.id());
  }

  private String nextChannelCursor(java.util.List<ChannelRow> rows, boolean hasMore) {
    if (!hasMore || rows.isEmpty()) {
      return null;
    }
    var last = rows.getLast();
    return PageCursor.encode(last.createdAt(), last.id());
  }
}
