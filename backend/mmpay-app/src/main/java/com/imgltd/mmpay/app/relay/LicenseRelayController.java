package com.imgltd.mmpay.app.relay;

import com.imgltd.mmpay.app.integrations.IntegrationService;
import com.imgltd.mmpay.app.integrations.RelayForwardResponse;
import com.imgltd.mmpay.system.DegradedModeGuard;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LicenseRelayController {
  private final IntegrationService service;
  private final String trustHeader;
  private final DegradedModeGuard degradedModeGuard;

  public LicenseRelayController(
      IntegrationService service,
      @Value("${mmpay.relay.trust-header:X-Client-Cert}") String trustHeader,
      DegradedModeGuard degradedModeGuard) {
    this.service = service;
    this.trustHeader = trustHeader;
    this.degradedModeGuard = degradedModeGuard;
  }

  @PostMapping(
      value = "/api/license-relay/v1/forward",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<RelayForwardResponse> forward(
      @RequestBody byte[] payload,
      @RequestHeader(value = "X-License-Relay-Target-Id", required = false) String targetId,
      @RequestHeader(value = "X-Request-Id", required = false) String requestId,
      HttpServletRequest request) {
    degradedModeGuard.requireWriteAllowed("license-relay-forward");
    String certificate = request.getHeader(trustHeader);
    if (certificate == null || certificate.isBlank()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.accepted().body(service.forward(payload, targetId, requestId, certificate));
  }
}
