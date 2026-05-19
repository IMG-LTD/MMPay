package com.imgltd.mmpay.app.integrations;

import com.imgltd.mmpay.merchant.ListResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationAdminController {
  private final IntegrationService service;

  public IntegrationAdminController(IntegrationService service) {
    this.service = service;
  }

  @PostMapping("/api/admin/integrations")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  IntegrationResponse create(@RequestBody IntegrationCreateRequest request, Authentication auth) {
    return service.createRelayIntegration(request, actor(auth));
  }

  @GetMapping("/api/admin/integrations")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  ListResponse<IntegrationResponse> list() {
    return service.listIntegrations();
  }

  @GetMapping("/api/admin/integrations/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  IntegrationResponse get(@PathVariable("id") String id) {
    return service.getIntegration(id);
  }

  @PostMapping("/api/admin/integrations/{id}/test")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  LicenseRelayLogResponse testIntegration(@PathVariable("id") String id, Authentication auth) {
    return service.testIntegration(id, actor(auth));
  }

  @PostMapping("/api/admin/license-relay/logs/{id}/redispatch")
  @PreAuthorize("hasAnyRole('ADMIN','OPS')")
  LicenseRelayLogResponse redispatchLog(@PathVariable("id") long id, Authentication auth) {
    return service.redispatchLog(id, actor(auth));
  }

  @PostMapping("/api/admin/relay/reload-trust")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> reloadTrust() {
    return ResponseEntity.status(501).build();
  }

  private String actor(Authentication auth) {
    return auth == null || auth.getName() == null ? "unknown" : auth.getName();
  }
}
