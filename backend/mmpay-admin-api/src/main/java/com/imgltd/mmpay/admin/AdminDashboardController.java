package com.imgltd.mmpay.admin;

import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminDashboardController {
  private final AdminDashboardReadService readService;

  public AdminDashboardController(AdminDashboardReadService readService) {
    this.readService = Objects.requireNonNull(readService, "readService");
  }

  @GetMapping("/api/admin/dashboard")
  @PreAuthorize("hasAnyRole('ADMIN','OPS','FINANCE','AUDITOR')")
  public AdminDashboardResponse getDashboard() {
    return readService.getDashboard();
  }
}
