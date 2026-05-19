package com.imgltd.mmpay.iam;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServicePrincipalController {
  private final ServicePrincipalService service;

  public ServicePrincipalController(ServicePrincipalService service) {
    this.service = service;
  }

  @PostMapping("/api/admin/service-principals")
  @PreAuthorize("hasRole('ADMIN')")
  @ResponseStatus(HttpStatus.CREATED)
  public ServicePrincipalResponse create(@RequestBody ServicePrincipalRequest request) {
    return service.create(request);
  }
}
