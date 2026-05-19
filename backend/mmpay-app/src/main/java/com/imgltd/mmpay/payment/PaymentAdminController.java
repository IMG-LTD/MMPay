package com.imgltd.mmpay.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.imgltd.mmpay.merchant.ListResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentAdminController {
  private final PaymentService service;
  private final ProviderCallbackVerifier callbackVerifier;
  private final ObjectMapper objectMapper;

  public PaymentAdminController(
      PaymentService service, ProviderCallbackVerifier callbackVerifier, ObjectMapper objectMapper) {
    this.service = service;
    this.callbackVerifier = callbackVerifier;
    this.objectMapper = objectMapper;
  }

  @PostMapping("/api/admin/payment-intents")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  PaymentIntentResponse createPaymentIntent(
      @RequestBody PaymentIntentCreateRequest request,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      Authentication auth) {
    return service.createIntent(request, idempotencyKey, actor(auth));
  }

  @GetMapping("/api/admin/payment-intents")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<PaymentIntentResponse> listPaymentIntents() {
    return service.listPaymentIntents();
  }

  @GetMapping("/api/admin/payment-intents/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  PaymentIntentResponse getPaymentIntent(@PathVariable("id") String id) {
    return service.getPaymentIntent(id);
  }

  @PostMapping("/api/admin/payment-intents/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  PaymentIntentResponse cancelPaymentIntent(@PathVariable("id") String id, Authentication auth) {
    return service.cancelPaymentIntent(id, actor(auth));
  }

  @PostMapping("/api/admin/refunds")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  ResponseEntity<RefundResponse> createRefund(@RequestBody RefundCreateRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createRefund(request, actor(auth)));
  }

  @GetMapping("/api/admin/refunds")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<RefundResponse> listRefunds() {
    return service.listRefunds();
  }

  @GetMapping("/api/admin/refunds/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  RefundResponse getRefund(@PathVariable("id") String id) {
    return service.getRefund(id);
  }

  @GetMapping("/api/admin/reconciliation/runs")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<ReconciliationRunResponse> listReconciliationRuns() {
    return service.listReconciliationRuns();
  }

  @PostMapping("/api/admin/reconciliation/runs/{id}/ack")
  @PreAuthorize("hasAnyRole('ADMIN','FINANCE')")
  ReconciliationRunResponse ackReconciliationRun(@PathVariable("id") long id, Authentication auth) {
    return service.ackReconciliationRun(id, actor(auth));
  }

  @PostMapping("/api/admin/webhook-out/integrations")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<WebhookIntegrationResponse> createWebhookIntegration(
      @RequestBody WebhookIntegrationCreateRequest request, Authentication auth) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.createWebhookIntegration(request, actor(auth)));
  }

  @PostMapping("/api/admin/webhook-out/bulk-redispatch")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  BulkRedispatchResponse bulkRedispatch(@RequestBody BulkRedispatchRequest request, Authentication auth) {
    return service.bulkRedispatch(request, actor(auth));
  }

  @GetMapping("/api/admin/webhook-out/delivery-logs")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<DeliveryLogResponse> listDeliveryLogs() {
    return service.listDeliveryLogs();
  }

  @GetMapping("/api/admin/webhook-out/delivery-logs/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  DeliveryLogResponse getDeliveryLog(@PathVariable("id") long id) {
    return service.getDeliveryLog(id);
  }

  @PostMapping("/api/admin/webhook-out/delivery-logs/{id}/redispatch")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  DeliveryLogResponse redispatchDeliveryLog(@PathVariable("id") long id, Authentication auth) {
    return service.redispatchDeliveryLog(id, actor(auth));
  }

  @PostMapping("/webhook-in/{providerCode}")
  ResponseEntity<PaymentIntentResponse> providerCallback(
      @PathVariable("providerCode") String providerCode,
      @RequestBody String body,
      @RequestHeader(value = "X-MMPay-Provider-Signature", required = false) String signature) {
    ProviderCallbackRequest request = readCallback(body);
    String signatureSha = callbackVerifier.requireValid(signature, body, request.occurredAt());
    return ResponseEntity.ok(service.acceptCallback(providerCode, request, signatureSha, "provider:" + providerCode));
  }

  private ProviderCallbackRequest readCallback(String body) {
    try {
      return objectMapper.readValue(body, ProviderCallbackRequest.class);
    } catch (JsonProcessingException exception) {
      throw PaymentProblems.unprocessable("urn:mmpay:problem:provider-callback-invalid", "callback body is invalid");
    }
  }

  private String actor(Authentication auth) {
    return auth == null || auth.getName() == null ? "unknown" : auth.getName();
  }
}
