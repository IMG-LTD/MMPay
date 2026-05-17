package com.imgltd.mmpay.admin;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AdminDashboardReadService {
  private static final String ZERO = "0";
  private static final String CREDENTIALS_REQUIRED = "credentials-required";

  public AdminDashboardResponse getDashboard() {
    return new AdminDashboardResponse(navigation(), metrics(), tables());
  }

  private static List<AdminDashboardResponse.NavigationItem> navigation() {
    return List.of(
        new AdminDashboardResponse.NavigationItem("merchants", "Merchants"),
        new AdminDashboardResponse.NavigationItem("channels", "Channels"),
        new AdminDashboardResponse.NavigationItem("orders", "Orders"),
        new AdminDashboardResponse.NavigationItem("refunds", "Refunds"),
        new AdminDashboardResponse.NavigationItem("webhook-logs", "Webhook logs"),
        new AdminDashboardResponse.NavigationItem("reconciliation", "Reconciliation"));
  }

  private static List<AdminDashboardResponse.MetricCard> metrics() {
    return List.of(
        new AdminDashboardResponse.MetricCard("Active merchants", ZERO),
        new AdminDashboardResponse.MetricCard("Enabled channels", ZERO),
        new AdminDashboardResponse.MetricCard("Today orders", ZERO),
        new AdminDashboardResponse.MetricCard("Pending refunds", ZERO));
  }

  private static List<AdminDashboardResponse.AdminTable> tables() {
    return List.of(
        credentialsTable(),
        channelsTable(),
        emptyTable("orders"),
        emptyTable("refunds"),
        emptyTable("webhook-logs"),
        emptyTable("reconciliation"));
  }

  private static AdminDashboardResponse.AdminTable credentialsTable() {
    return new AdminDashboardResponse.AdminTable(
        "credentials",
        List.of("label", "valueKind"),
        List.of(secretField("Merchant ID"), secretField("API Key"), secretField("Webhook Secret")));
  }

  private static AdminDashboardResponse.AdminTable channelsTable() {
    return new AdminDashboardResponse.AdminTable(
        "channels",
        List.of("merchant", "channel", "status", "updatedAt"),
        List.of(Map.of(
            "merchant", "MMMail",
            "channel", "Huifu sandbox",
            "status", CREDENTIALS_REQUIRED,
            "updatedAt", "not connected")));
  }

  private static AdminDashboardResponse.AdminTable emptyTable(String key) {
    return new AdminDashboardResponse.AdminTable(key, List.of(), List.of());
  }

  private static Map<String, String> secretField(String label) {
    return Map.of("label", label, "valueKind", "secret-handle");
  }
}
