package com.imgltd.mmpay.app.integrations;

import com.imgltd.mmpay.audit.AuditWriter;
import com.imgltd.mmpay.license.LicenseRelay;
import com.imgltd.mmpay.license.RelayConfig;
import com.imgltd.mmpay.merchant.AdminProblemException;
import com.imgltd.mmpay.merchant.ListResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IntegrationService {
  private static final int MAX_PAYLOAD_BYTES = 65536;
  private static final Pattern SLUG = Pattern.compile("^[a-z][a-z0-9-]{2,63}$");
  private static final byte[] SYNTHETIC_PAYLOAD = "mmpay-license-relay-synthetic-test".getBytes(StandardCharsets.UTF_8);
  private final IntegrationRepository repository;
  private final AuditWriter auditWriter;
  private final Clock clock;
  private final TargetUrlPolicy targetUrlPolicy;
  private final RelayRateLimiter rateLimiter;
  private final int responseCapBytes;

  public IntegrationService(
      IntegrationRepository repository,
      AuditWriter auditWriter,
      Clock clock,
      @Value("${mmpay.relay.allow-local-targets:false}") boolean allowLocalTargets,
      @Value("${mmpay.relay.rate-limit-per-cert:60}") int rateLimitPerCert,
      @Value("${mmpay.relay.response-cap-bytes:8192}") int responseCapBytes) {
    this.repository = repository;
    this.auditWriter = auditWriter;
    this.clock = clock;
    this.targetUrlPolicy = new TargetUrlPolicy(allowLocalTargets);
    this.rateLimiter = new RelayRateLimiter(rateLimitPerCert);
    this.responseCapBytes = responseCapBytes;
  }

  public IntegrationResponse createRelayIntegration(IntegrationCreateRequest request, String actor) {
    validateRelayRequest(request);
    repository.createRelayIntegration(request, fingerprint(request.secretRef()), clock.instant());
    audit(actor, "integration.create", "integration", request.id(), accepted());
    return IntegrationResponse.from(repository.requireIntegration(request.id()));
  }

  public ListResponse<IntegrationResponse> listIntegrations() {
    var items = repository.listIntegrations().stream().map(IntegrationResponse::from).toList();
    return new ListResponse<>(items, null, false);
  }

  public IntegrationResponse getIntegration(String id) {
    return IntegrationResponse.from(repository.requireIntegration(id));
  }

  public RelayForwardResponse forward(byte[] payload, String targetId, String requestId, String certificate) {
    requireForwardRequest(payload, targetId, requestId);
    if (!rateLimiter.allow(certificate)) {
      audit("vendor", "license_relay.rate_limited", "license_relay", targetId, rejected("rate_limited"));
      throw problem("urn:mmpay:problem:relay-rate-limited", HttpStatus.TOO_MANY_REQUESTS, "relay rate limit exceeded");
    }
    var target = repository.requireActiveRelayTarget(targetId);
    var receipt = new LicenseRelay(relayConfig(target)).forward(payload, target.id(), requestId);
    var row = repository.insertForwardLog(receipt, false, clock.instant());
    auditForwardResult(target.id(), receipt.success(), receipt.errorClass());
    if (!receipt.success()) {
      throw problem("urn:mmpay:problem:relay-dispatch-failed", HttpStatus.BAD_GATEWAY, receipt.errorClass());
    }
    return RelayForwardResponse.from(row);
  }

  public LicenseRelayLogResponse testIntegration(String id, String actor) {
    repository.requireActiveRelayTarget(id);
    var row = repository.insertSyntheticLog(id, sha256(SYNTHETIC_PAYLOAD), clock.instant());
    audit(actor, "integration.test_invoked", "integration", id, accepted());
    audit(actor, "license_relay.synthetic_forwarded", "license_relay", id, accepted());
    return LicenseRelayLogResponse.from(row);
  }

  public LicenseRelayLogResponse redispatchLog(long id, String actor) {
    var row = repository.redispatchSyntheticLog(id, clock.instant());
    audit(actor, "license_relay.redispatch", "license_relay", row.targetId(), accepted());
    return LicenseRelayLogResponse.from(row);
  }

  public long successfulRelayForwardCount() {
    return repository.successfulForwardCount();
  }

  private void validateRelayRequest(IntegrationCreateRequest request) {
    if (!"relay".equals(IntegrationInput.requireText(request.kind(), "kind"))) {
      throw IntegrationInput.problem("urn:mmpay:problem:integration-kind-invalid", "integration kind must be relay");
    }
    IntegrationInput.requireText(request.id(), "id");
    IntegrationInput.requireText(request.name(), "name");
    IntegrationInput.requireText(request.secretRef(), "secret_ref");
    requireSlug(request.slug());
    targetUrlPolicy.requireAcceptable(request.targetUrl());
  }

  private void requireSlug(String slug) {
    String value = IntegrationInput.requireText(slug, "slug");
    if (!SLUG.matcher(value).matches()) {
      throw IntegrationInput.problem("urn:mmpay:problem:integration-slug-invalid", "integration slug is invalid");
    }
    if (value.equals("admin") || value.startsWith("admin-") || value.startsWith("billing-")) {
      throw IntegrationInput.problem("urn:mmpay:problem:integration-slug-reserved", "integration slug is reserved");
    }
  }

  private void requireForwardRequest(byte[] payload, String targetId, String requestId) {
    IntegrationInput.requireText(targetId, "X-License-Relay-Target-Id");
    IntegrationInput.requireText(requestId, "X-Request-Id");
    if (payload == null || payload.length == 0) {
      throw problem("urn:mmpay:problem:relay-payload-empty", HttpStatus.UNPROCESSABLE_ENTITY, "payload is empty");
    }
    if (payload.length > MAX_PAYLOAD_BYTES) {
      throw problem("urn:mmpay:problem:relay-payload-too-large", HttpStatus.PAYLOAD_TOO_LARGE, "payload is too large");
    }
  }

  private RelayConfig relayConfig(RelayTargetRow target) {
    URI targetUri = targetUrlPolicy.requireAcceptable(target.targetUrl());
    return new RelayConfig(Map.of(target.id(), targetUri), responseCapBytes, Duration.ofSeconds(5));
  }

  private void auditForwardResult(String targetId, boolean success, String reason) {
    String action = success ? "license_relay.forwarded" : "license_relay.failed";
    audit("vendor", action, "license_relay", targetId, success ? accepted() : rejected(reason));
  }

  private void audit(String actor, String action, String kind, String id, Map<String, ?> details) {
    auditWriter.emit("user", actor, action, kind, id, details);
  }

  private AdminProblemException problem(String type, HttpStatus status, String detail) {
    return new AdminProblemException(type, status, detail);
  }

  private Map<String, Object> accepted() {
    return details("result", "accepted");
  }

  private Map<String, Object> rejected(String reason) {
    return details("result", "rejected", "reason", reason);
  }

  private Map<String, Object> details(Object... entries) {
    var map = new LinkedHashMap<String, Object>();
    for (int index = 0; index < entries.length; index += 2) {
      map.put(entries[index].toString(), entries[index + 1]);
    }
    return map;
  }

  private String fingerprint(String value) {
    return sha256(IntegrationInput.requireText(value, "secret_ref").getBytes(StandardCharsets.UTF_8)).substring(0, 16);
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("sha256 unavailable", exception);
    }
  }
}
